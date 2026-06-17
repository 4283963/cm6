package com.crossborder.fx.rate;

import com.crossborder.fx.common.Currency;
import com.crossborder.fx.entity.ExchangeRate;

import java.util.List;

public interface ExchangeRateProvider {

    ExchangeRate fetchRate(Currency baseCurrency, Currency quoteCurrency);

    List<ExchangeRate> fetchAllRates();

    String getProviderName();
}
