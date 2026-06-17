package com.crossborder.fx.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "routing_decision", uniqueConstraints = {
        @UniqueConstraint(name = "uk_decision_id", columnNames = {"decision_id"})
})
public class RoutingDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "decision_id", nullable = false, length = 64)
    private String decisionId;

    @Column(name = "inquiry_id", nullable = false, length = 64)
    private String inquiryId;

    @Column(name = "request_no", nullable = false, length = 64)
    private String requestNo;

    @Column(name = "seller_id", nullable = false, length = 64)
    private String sellerId;

    @Column(name = "original_currency", nullable = false, length = 8)
    private String originalCurrency;

    @Column(name = "target_currency", nullable = false, length = 8)
    private String targetCurrency;

    @Column(name = "original_amount", precision = 18, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "selected_bank_code", nullable = false, length = 32)
    private String selectedBankCode;

    @Column(name = "selected_bank_name", nullable = false, length = 128)
    private String selectedBankName;

    @Column(name = "selected_buy_price", precision = 18, scale = 6)
    private BigDecimal selectedBuyPrice;

    @Column(name = "selected_target_amount", precision = 18, scale = 2)
    private BigDecimal selectedTargetAmount;

    @Column(name = "total_quoted_banks")
    private Integer totalQuotedBanks;

    @Column(name = "success_quoted_banks")
    private Integer successQuotedBanks;

    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    @Column(name = "all_quotes_summary", columnDefinition = "TEXT")
    private String allQuotesSummary;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
    }

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

    public Integer getTotalQuotedBanks() {
        return totalQuotedBanks;
    }

    public void setTotalQuotedBanks(Integer totalQuotedBanks) {
        this.totalQuotedBanks = totalQuotedBanks;
    }

    public Integer getSuccessQuotedBanks() {
        return successQuotedBanks;
    }

    public void setSuccessQuotedBanks(Integer successQuotedBanks) {
        this.successQuotedBanks = successQuotedBanks;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public String getAllQuotesSummary() {
        return allQuotesSummary;
    }

    public void setAllQuotesSummary(String allQuotesSummary) {
        this.allQuotesSummary = allQuotesSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
