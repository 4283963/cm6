package com.crossborder.fx.rate;

import com.crossborder.fx.common.Currency;
import com.crossborder.fx.entity.ExchangeRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MockExchangeRateProvider implements ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(MockExchangeRateProvider.class);

    @Value("${fx.rate.simulation-mode:true}")
    private boolean simulationMode;

    private static final BigDecimal USD_CNY_MID = new BigDecimal("7.2450");
    private static final BigDecimal EUR_CNY_MID = new BigDecimal("7.8650");
    private static final BigDecimal GBP_CNY_MID = new BigDecimal("9.1580");
    private static final BigDecimal JPY_CNY_MID = new BigDecimal("0.0485");
    private static final BigDecimal HKD_CNY_MID = new BigDecimal("0.9285");
    private static final BigDecimal AUD_CNY_MID = new BigDecimal("4.7820");

    private static final BigDecimal SPREAD = new BigDecimal("0.0015");

    @Override
    public ExchangeRate fetchRate(Currency baseCurrency, Currency quoteCurrency) {
        log.debug("Fetching exchange rate from {} to {}", baseCurrency, quoteCurrency);

        BigDecimal midPrice = getMidPrice(baseCurrency, quoteCurrency);
        BigDecimal spread = midPrice.multiply(SPREAD).setScale(6, RoundingMode.HALF_UP);

        double fluctuation = ThreadLocalRandom.current().nextDouble(-0.002, 0.002);
        BigDecimal fluctuationAmount = midPrice.multiply(new BigDecimal(fluctuation)).setScale(6, RoundingMode.HALF_UP);
        BigDecimal actualMid = midPrice.add(fluctuationAmount);

        BigDecimal buyPrice = actualMid.subtract(spread).setScale(6, RoundingMode.HALF_UP);
        BigDecimal sellPrice = actualMid.add(spread).setScale(6, RoundingMode.HALF_UP);

        ExchangeRate rate = new ExchangeRate();
        rate.setBaseCurrency(baseCurrency);
        rate.setQuoteCurrency(quoteCurrency);
        rate.setBuyPrice(buyPrice);
        rate.setSellPrice(sellPrice);
        rate.setMidPrice(actualMid.setScale(6, RoundingMode.HALF_UP));
        rate.setSource(getProviderName());
        rate.setTimestamp(LocalDateTime.now());

        log.info("Fetched rate: {}/{} buy={}, sell={}, mid={}",
                baseCurrency, quoteCurrency, buyPrice, sellPrice, rate.getMidPrice());

        return rate;
    }

    @Override
    public List<ExchangeRate> fetchAllRates() {
        List<ExchangeRate> rates = new ArrayList<>();
        Currency[] majors = {Currency.USD, Currency.EUR, Currency.GBP, Currency.JPY, Currency.HKD, Currency.AUD};

        for (Currency base : majors) {
            rates.add(fetchRate(base, Currency.CNY));
        }

        return rates;
    }

    @Override
    public String getProviderName() {
        return simulationMode ? "MOCK_FX_CENTER" : "REAL_FX_CENTER";
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
