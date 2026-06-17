package com.crossborder.fx.common;

public enum Currency {
    CNY("CNY", "人民币"),
    USD("USD", "美元"),
    EUR("EUR", "欧元"),
    GBP("GBP", "英镑"),
    JPY("JPY", "日元"),
    HKD("HKD", "港币"),
    AUD("AUD", "澳元");

    private final String code;
    private final String description;

    Currency(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
