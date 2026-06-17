package com.crossborder.fx.dto;

import com.crossborder.fx.common.Currency;

import java.math.BigDecimal;

public class CurrencyExposureDTO {
    private Currency currency;
    private BigDecimal netPosition;
    private BigDecimal exposureAmountCny;
    private BigDecimal exposureLimitCny;
    private BigDecimal exposureRatio;
    private Boolean circuitBreakerTriggered;

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getNetPosition() {
        return netPosition;
    }

    public void setNetPosition(BigDecimal netPosition) {
        this.netPosition = netPosition;
    }

    public BigDecimal getExposureAmountCny() {
        return exposureAmountCny;
    }

    public void setExposureAmountCny(BigDecimal exposureAmountCny) {
        this.exposureAmountCny = exposureAmountCny;
    }

    public BigDecimal getExposureLimitCny() {
        return exposureLimitCny;
    }

    public void setExposureLimitCny(BigDecimal exposureLimitCny) {
        this.exposureLimitCny = exposureLimitCny;
    }

    public BigDecimal getExposureRatio() {
        return exposureRatio;
    }

    public void setExposureRatio(BigDecimal exposureRatio) {
        this.exposureRatio = exposureRatio;
    }

    public Boolean getCircuitBreakerTriggered() {
        return circuitBreakerTriggered;
    }

    public void setCircuitBreakerTriggered(Boolean circuitBreakerTriggered) {
        this.circuitBreakerTriggered = circuitBreakerTriggered;
    }
}
