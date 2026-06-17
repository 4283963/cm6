package com.crossborder.fx.entity;

import com.crossborder.fx.common.Currency;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller")
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", unique = true, nullable = false, length = 64)
    private String sellerId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_currency", nullable = false)
    private Currency settlementCurrency;

    @Column(name = "daily_settlement_limit", precision = 18, scale = 2)
    private BigDecimal dailySettlementLimit;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (settlementCurrency == null) {
            settlementCurrency = Currency.CNY;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Currency getSettlementCurrency() {
        return settlementCurrency;
    }

    public void setSettlementCurrency(Currency settlementCurrency) {
        this.settlementCurrency = settlementCurrency;
    }

    public BigDecimal getDailySettlementLimit() {
        return dailySettlementLimit;
    }

    public void setDailySettlementLimit(BigDecimal dailySettlementLimit) {
        this.dailySettlementLimit = dailySettlementLimit;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
