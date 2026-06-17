package com.crossborder.fx.rate;

import com.crossborder.fx.common.Currency;
import com.crossborder.fx.entity.ExchangeRate;
import com.crossborder.fx.repository.ExchangeRateRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private final ExchangeRateProvider rateProvider;
    private final ExchangeRateRepository rateRepository;

    @Value("${fx.rate.cache-duration-seconds:30}")
    private int cacheDurationSeconds;

    private final Map<String, CachedRate> rateCache = new ConcurrentHashMap<>();

    public ExchangeRateService(ExchangeRateProvider rateProvider, ExchangeRateRepository rateRepository) {
        this.rateProvider = rateProvider;
        this.rateRepository = rateRepository;
    }

    private static class CachedRate {
        final ExchangeRate rate;
        final LocalDateTime cachedAt;

        CachedRate(ExchangeRate rate) {
            this.rate = rate;
            this.cachedAt = LocalDateTime.now();
        }

        boolean isExpired(int cacheDurationSeconds) {
            return cachedAt.plusSeconds(cacheDurationSeconds).isBefore(LocalDateTime.now());
        }
    }

    @PostConstruct
    public void init() {
        log.info("Initializing exchange rate service with provider: {}", rateProvider.getProviderName());
        refreshAllRates();
    }

    @Scheduled(fixedDelayString = "${fx.rate.cache-duration-seconds:30}000")
    public void refreshAllRates() {
        log.info("Refreshing all exchange rates...");
        try {
            List<ExchangeRate> rates = rateProvider.fetchAllRates();
            rates.forEach(rate -> {
                rateRepository.save(rate);
                String key = buildCacheKey(rate.getBaseCurrency(), rate.getQuoteCurrency());
                rateCache.put(key, new CachedRate(rate));
                log.info("Updated rate: {}/{} = {}", rate.getBaseCurrency(), rate.getQuoteCurrency(), rate.getMidPrice());
            });
            log.info("Successfully refreshed {} exchange rates", rates.size());
        } catch (Exception e) {
            log.error("Failed to refresh exchange rates", e);
        }
    }

    public ExchangeRate getRate(Currency baseCurrency, Currency quoteCurrency) {
        String key = buildCacheKey(baseCurrency, quoteCurrency);
        CachedRate cached = rateCache.get(key);

        if (cached != null && !cached.isExpired(cacheDurationSeconds)) {
            log.debug("Returning cached rate for {}/{}", baseCurrency, quoteCurrency);
            return cached.rate;
        }

        return fetchAndCacheRate(baseCurrency, quoteCurrency);
    }

    public ExchangeRate getLatestRate(Currency baseCurrency, Currency quoteCurrency) {
        return fetchAndCacheRate(baseCurrency, quoteCurrency);
    }

    private synchronized ExchangeRate fetchAndCacheRate(Currency base, Currency quote) {
        String key = buildCacheKey(base, quote);
        CachedRate cached = rateCache.get(key);
        if (cached != null && !cached.isExpired(cacheDurationSeconds)) {
            return cached.rate;
        }

        log.info("Fetching fresh rate for {}/{} from provider", base, quote);
        ExchangeRate rate = rateProvider.fetchRate(base, quote);
        rateRepository.save(rate);
        rateCache.put(key, new CachedRate(rate));

        return rate;
    }

    public BigDecimal convert(Currency from, Currency to, BigDecimal amount) {
        if (from == to) {
            return amount;
        }
        ExchangeRate rate = getRate(from, to);
        return amount.multiply(rate.getMidPrice()).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal convertWithBuyRate(Currency from, Currency to, BigDecimal amount) {
        if (from == to) {
            return amount;
        }
        ExchangeRate rate = getRate(from, to);
        return amount.multiply(rate.getBuyPrice()).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal convertWithSellRate(Currency from, Currency to, BigDecimal amount) {
        if (from == to) {
            return amount;
        }
        ExchangeRate rate = getRate(from, to);
        return amount.multiply(rate.getSellPrice()).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public List<ExchangeRate> getAllCurrentRates() {
        return rateCache.values().stream()
                .map(c -> c.rate)
                .toList();
    }

    private String buildCacheKey(Currency base, Currency quote) {
        return base.name() + "_" + quote.name();
    }
}
