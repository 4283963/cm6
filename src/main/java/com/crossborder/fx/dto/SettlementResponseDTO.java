package com.crossborder.fx.dto;

import com.crossborder.fx.common.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SettlementResponseDTO {
    private String requestNo;
    private String sellerId;
    private String originalCurrency;
    private String targetCurrency;
    private BigDecimal originalAmount;
    private BigDecimal targetAmount;
    private BigDecimal exchangeRate;
    private SettlementStatus status;
    private Boolean riskCheckPassed;
    private String riskReason;
    private LocalDateTime createdAt;
    private String selectedBankCode;
    private String selectedBankName;
    private String inquiryId;

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public void setTargetCurrency(String targetCurrency) {
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

    public SettlementStatus getStatus() {
        return status;
    }

    public void setStatus(SettlementStatus status) {
        this.status = status;
    }

    public Boolean getRiskCheckPassed() {
        return riskCheckPassed;
    }

    public void setRiskCheckPassed(Boolean riskCheckPassed) {
        this.riskCheckPassed = riskCheckPassed;
    }

    public String getRiskReason() {
        return riskReason;
    }

    public void setRiskReason(String riskReason) {
        this.riskReason = riskReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public String getInquiryId() {
        return inquiryId;
    }

    public void setInquiryId(String inquiryId) {
        this.inquiryId = inquiryId;
    }
}
