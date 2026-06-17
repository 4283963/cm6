package com.crossborder.fx.rate;

import com.crossborder.fx.common.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class MultiBankQuoteService {

    private static final Logger log = LoggerFactory.getLogger(MultiBankQuoteService.class);
    private static final long QUOTE_TIMEOUT_SECONDS = 5;

    private final List<BankQuoteProvider> providers;
    private final ExecutorService executor;

    public MultiBankQuoteService(List<BankQuoteProvider> providers) {
        this.providers = providers;
        this.executor = Executors.newFixedThreadPool(providers.size(), r -> {
            Thread t = new Thread(r, "bank-quote-worker");
            t.setDaemon(true);
            return t;
        });
        log.info("Initialized MultiBankQuoteService with {} providers: {}",
                providers.size(),
                providers.stream().map(BankQuoteProvider::getBankCode).toList());
    }

    /**
     * 异步并发询价：同时向所有银行发起询价，等待全部返回或超时
     */
    public List<BankQuote> fetchQuotesAsync(Currency baseCurrency, Currency quoteCurrency) {
        log.info("Starting async quote fetching for {}/{} from {} banks",
                baseCurrency, quoteCurrency, providers.size());

        List<CompletableFuture<BankQuote>> futures = new ArrayList<>();

        for (BankQuoteProvider provider : providers) {
            CompletableFuture<BankQuote> future = CompletableFuture.supplyAsync(
                () -> {
                    long start = System.currentTimeMillis();
                    try {
                        BankQuote quote = provider.fetchQuote(baseCurrency, quoteCurrency);
                        quote.setResponseTimeMs(System.currentTimeMillis() - start);
                        log.info("Quote from {}: buy={}, sell={}, mid={}, time={}ms",
                                provider.getBankCode(), quote.getBuyPrice(),
                                quote.getSellPrice(), quote.getMidPrice(),
                                quote.getResponseTimeMs());
                        return quote;
                    } catch (Exception e) {
                        log.error("Quote failed from {}: {}", provider.getBankCode(), e.getMessage());
                        BankQuote failQuote = new BankQuote();
                        failQuote.setBankCode(provider.getBankCode());
                        failQuote.setBankName(provider.getBankName());
                        failQuote.setBaseCurrency(baseCurrency);
                        failQuote.setQuoteCurrency(quoteCurrency);
                        failQuote.setSuccess(false);
                        failQuote.setErrorMessage(e.getMessage());
                        failQuote.setResponseTimeMs(System.currentTimeMillis() - start);
                        return failQuote;
                    }
                },
                executor
            );
            futures.add(future);
        }

        // 等待所有询价完成，超时5秒
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(QUOTE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Some bank quotes timed out after {} seconds", QUOTE_TIMEOUT_SECONDS);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error waiting for bank quotes", e);
        }

        List<BankQuote> results = new ArrayList<>();
        for (CompletableFuture<BankQuote> future : futures) {
            try {
                if (future.isDone()) {
                    results.add(future.get());
                } else {
                    future.cancel(true);
                    log.warn("A bank quote was cancelled due to timeout");
                }
            } catch (Exception e) {
                log.error("Error getting quote result", e);
            }
        }

        log.info("Completed quote fetching: {} responses ({} successful, {} failed)",
                results.size(),
                results.stream().filter(BankQuote::isSuccess).count(),
                results.stream().filter(q -> !q.isSuccess()).count());

        return results;
    }

    public List<BankQuoteProvider> getProviders() {
        return providers;
    }
}
