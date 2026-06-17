package com.crossborder.fx.dto;

import com.crossborder.fx.common.Currency;

import java.math.BigDecimal;
import java.util.List;

public class MultiBankQuoteResultDTO {

    private String inquiryId;
    private String requestNo;
    private String sellerId;
    private Currency originalCurrency;
    private Currency targetCurrency;
    private BigDecimal originalAmount;
    private List<BankQuoteDTO> quotes;
    private String selectedBankCode;
    private String selectedBankName;
    private BigDecimal selectedBuyPrice;
    private BigDecimal selectedTargetAmount;
    private String decisionReason;

    public String getInquiryId() {
        return inquiryId;
    }

    public void setInquiryId(String inquiryId) {
        this.inquiryId = inquiryId;
    }

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

    public List<BankQuoteDTO> getQuotes() {
        return quotes;
    }

    public void setQuotes(List<BankQuoteDTO> quotes) {
        this.quotes = quotes;
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

    public BigDecimal getSelectedBuyPrice() {
        return selectedBuyPrice;
    }

    public void setSelectedBuyPrice(BigDecimal selectedBuyPrice) {
        this.selectedBuyPrice = selectedBuyPrice;
    }

    public BigDecimal getSelectedTargetAmount() {
        return selectedTargetAmount;
    }

    public void setSelectedTargetAmount(BigDecimal selectedTargetAmount) {
        this.selectedTargetAmount = selectedTargetAmount;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }
}
