package com.crossborder.fx.rate;

import com.crossborder.fx.common.Currency;

public interface BankQuoteProvider {
    BankQuote fetchQuote(Currency baseCurrency, Currency quoteCurrency);
    String getBankCode();
    String getBankName();
}
