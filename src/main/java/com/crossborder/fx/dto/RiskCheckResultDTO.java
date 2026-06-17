package com.crossborder.fx.dto;

import com.crossborder.fx.common.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class RiskCheckResultDTO {

    private String sellerId;
    private Currency originalCurrency;
    private Currency targetCurrency;
    private BigDecimal originalAmount;
    private BigDecimal targetAmount;
    private BigDecimal exchangeRate;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private BigDecimal midPrice;
    private Map<Currency, CurrencyExposureDTO> exposures;
    private BigDecimal totalExposureCny;
    private BigDecimal totalExposureLimitCny;
    private BigDecimal exposureRatio;
    private boolean riskCheckPassed;
    private boolean circuitBreakerTriggered;
    private String riskReason;
    private LocalDateTime checkTime;
    private String inquiryId;
    private String selectedBankCode;
    private String selectedBankName;
    private List<BankQuoteDTO> bankQuotes;

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

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public BigDecimal getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(BigDecimal buyPrice) {
        this.buyPrice = buyPrice;
    }

    public BigDecimal getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(BigDecimal sellPrice) {
        this.sellPrice = sellPrice;
    }

    public BigDecimal getMidPrice() {
        return midPrice;
    }

    public void setMidPrice(BigDecimal midPrice) {
        this.midPrice = midPrice;
    }

    public Map<Currency, CurrencyExposureDTO> getExposures() {
        return exposures;
    }

    public void setExposures(Map<Currency, CurrencyExposureDTO> exposures) {
        this.exposures = exposures;
    }

    public BigDecimal getTotalExposureCny() {
        return totalExposureCny;
    }

    public void setTotalExposureCny(BigDecimal totalExposureCny) {
        this.totalExposureCny = totalExposureCny;
    }

    public BigDecimal getTotalExposureLimitCny() {
        return totalExposureLimitCny;
    }

    public void setTotalExposureLimitCny(BigDecimal totalExposureLimitCny) {
        this.totalExposureLimitCny = totalExposureLimitCny;
    }

    public BigDecimal getExposureRatio() {
        return exposureRatio;
    }

    public void setExposureRatio(BigDecimal exposureRatio) {
        this.exposureRatio = exposureRatio;
    }

    public boolean isRiskCheckPassed() {
        return riskCheckPassed;
    }

    public void setRiskCheckPassed(boolean riskCheckPassed) {
        this.riskCheckPassed = riskCheckPassed;
    }

    public boolean isCircuitBreakerTriggered() {
        return circuitBreakerTriggered;
    }

    public void setCircuitBreakerTriggered(boolean circuitBreakerTriggered) {
        this.circuitBreakerTriggered = circuitBreakerTriggered;
    }

    public String getRiskReason() {
        return riskReason;
    }

    public void setRiskReason(String riskReason) {
        this.riskReason = riskReason;
    }

    public LocalDateTime getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(LocalDateTime checkTime) {
        this.checkTime = checkTime;
    }

    public String getInquiryId() {
        return inquiryId;
    }

    public void setInquiryId(String inquiryId) {
        this.inquiryId = inquiryId;
    }

    public String getSelectedBankCode() {
        return selectedBankCode;
    }

    public void setSelectedBankCode(String selectedBankCode) {
        this.selectedBankCode = selectedBankCode;
    }

    public String getSelectedBankName() {
        return selectedBankName;
    }

    public void setSelectedBankName(String selectedBankName) {
        this.selectedBankName = selectedBankName;
    }

    public List<BankQuoteDTO> getBankQuotes() {
        return bankQuotes;
    }

    public void setBankQuotes(List<BankQuoteDTO> bankQuotes) {
        this.bankQuotes = bankQuotes;
    }
}
