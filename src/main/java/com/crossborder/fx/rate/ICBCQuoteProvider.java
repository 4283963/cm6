package com.crossborder.fx.rate;

import com.crossborder.fx.common.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ICBCQuoteProvider implements BankQuoteProvider {

    private static final Logger log = LoggerFactory.getLogger(ICBCQuoteProvider.class);

    private static final BigDecimal USD_CNY_MID = new BigDecimal("7.2450");
    private static final BigDecimal EUR_CNY_MID = new BigDecimal("7.8650");
    private static final BigDecimal GBP_CNY_MID = new BigDecimal("9.1580");
    private static final BigDecimal JPY_CNY_MID = new BigDecimal("0.0485");
    private static final BigDecimal HKD_CNY_MID = new BigDecimal("0.9285");
    private static final BigDecimal AUD_CNY_MID = new BigDecimal("4.7820");

    private static final BigDecimal SPREAD_RATE = new BigDecimal("0.0015");
    private static final BigDecimal BUY_OFFSET = new BigDecimal("0.0005");
    private static final BigDecimal SELL_OFFSET = new BigDecimal("-0.0003");
    private static final BigDecimal FLUCTUATION_RANGE = new BigDecimal("0.0015");

    private static final int MIN_DELAY_MS = 50;
    private static final int MAX_DELAY_MS = 200;
    private static final double FAILURE_RATE = 0.02;

    @Override
    public BankQuote fetchQuote(Currency baseCurrency, Currency quoteCurrency) {
        long startTime = System.currentTimeMillis();
        BankQuote quote = new BankQuote();
        quote.setBankCode(getBankCode());
        quote.setBankName(getBankName());
        quote.setBaseCurrency(baseCurrency);
        quote.setQuoteCurrency(quoteCurrency);

        try {
            simulateDelay();

            if (ThreadLocalRandom.current().nextDouble() < FAILURE_RATE) {
                throw new RuntimeException("ICBC 询价请求失败：网络超时");
            }

            BigDecimal midPrice = getMidPrice(baseCurrency, quoteCurrency);
            BigDecimal spread = midPrice.multiply(SPREAD_RATE).setScale(6, RoundingMode.HALF_UP);

            double fluctuation = ThreadLocalRandom.current().nextDouble(
                    FLUCTUATION_RANGE.negate().doubleValue(), FLUCTUATION_RANGE.doubleValue());
            BigDecimal fluctuationAmount = midPrice.multiply(new BigDecimal(fluctuation))
                    .setScale(6, RoundingMode.HALF_UP);
            BigDecimal actualMid = midPrice.add(fluctuationAmount).setScale(6, RoundingMode.HALF_UP);

            BigDecimal buyPrice = actualMid.subtract(spread).add(BUY_OFFSET).setScale(6, RoundingMode.HALF_UP);
            BigDecimal sellPrice = actualMid.add(spread).add(SELL_OFFSET).setScale(6, RoundingMode.HALF_UP);

            quote.setMidPrice(actualMid);
            quote.setBuyPrice(buyPrice);
            quote.setSellPrice(sellPrice);
            quote.setQuoteTime(LocalDateTime.now());
            quote.setSuccess(true);

            log.info("ICBC 询价成功: {}/{} buy={}, sell={}, mid={}",
                    baseCurrency, quoteCurrency, buyPrice, sellPrice, actualMid);
        } catch (RuntimeException e) {
            quote.setSuccess(false);
            quote.setErrorMessage(e.getMessage());
            quote.setQuoteTime(LocalDateTime.now());
            log.warn("ICBC 询价失败: {}/{} - {}", baseCurrency, quoteCurrency, e.getMessage());
        }

        quote.setResponseTimeMs(System.currentTimeMillis() - startTime);
        return quote;
    }

    @Override
    public String getBankCode() {
        return "ICBC";
    }

    @Override
    public String getBankName() {
        return "中国工商银行";
    }

    private void simulateDelay() {
        try {
            int delay = ThreadLocalRandom.current().nextInt(MIN_DELAY_MS, MAX_DELAY_MS + 1);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private BigDecimal getMidPrice(Currency base, Currency quote) {
        if (base == quote) {
            return BigDecimal.ONE;
        }

        if (quote == Currency.CNY) {
            return switch (base) {
                case USD -> USD_CNY_MID;
                case EUR -> EUR_CNY_MID;
                case GBP -> GBP_CNY_MID;
                case JPY -> JPY_CNY_MID;
                case HKD -> HKD_CNY_MID;
                case AUD -> AUD_CNY_MID;
                default -> throw new IllegalArgumentException("Unsupported currency pair: " + base + "/" + quote);
            };
        }

        if (base == Currency.CNY) {
            BigDecimal cnyMid = getMidPrice(quote, Currency.CNY);
            return BigDecimal.ONE.divide(cnyMid, 6, RoundingMode.HALF_UP);
        }

        BigDecimal baseToCny = getMidPrice(base, Currency.CNY);
        BigDecimal quoteToCny = getMidPrice(quote, Currency.CNY);
        return baseToCny.divide(quoteToCny, 6, RoundingMode.HALF_UP);
    }
}
