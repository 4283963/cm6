package com.crossborder.fx.entity;

import com.crossborder.fx.common.Currency;
import com.crossborder.fx.common.SettlementStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_request")
public class SettlementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_no", unique = true, nullable = false, length = 64)
    private String requestNo;

    @Column(name = "seller_id", nullable = false, length = 64)
    private String sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_currency", nullable = false)
    private Currency originalCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_currency", nullable = false)
    private Currency targetCurrency;

    @Column(name = "original_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "target_amount", precision = 18, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "exchange_rate", precision = 18, scale = 6)
    private BigDecimal exchangeRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SettlementStatus status;

    @Column(name = "risk_check_passed")
    private Boolean riskCheckPassed;

    @Column(name = "risk_reason", length = 512)
    private String riskReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = SettlementStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
