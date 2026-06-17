package com.crossborder.fx.dto;

import com.crossborder.fx.common.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class SettlementRequestDTO {

    @NotBlank(message = "卖家ID不能为空")
    private String sellerId;

    @NotNull(message = "原始币种不能为空")
    private Currency originalCurrency;

    private Currency targetCurrency = Currency.CNY;

    @NotNull(message = "结汇金额不能为空")
    @Positive(message = "结汇金额必须大于0")
    private BigDecimal originalAmount;

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public Currency getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(Currency originalCurrency) {
        this.originalCurrency = originalCurrency;
    }

    public Currency getTargetCurrency() {
        return targetCurrency;
    }

    public void setTargetCurrency(Currency targetCurrency) {
        this.targetCurrency = targetCurrency;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }
}
