package com.crossborder.fx.entity;

import com.crossborder.fx.common.Currency;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "position_exposure")
public class PositionExposure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private Currency currency;

    @Column(name = "net_position", nullable = false, precision = 18, scale = 2)
    private BigDecimal netPosition;

    @Column(name = "exposure_amount_cny", nullable = false, precision = 18, scale = 2)
    private BigDecimal exposureAmountCny;

    @Column(name = "exposure_limit_cny", nullable = false, precision = 18, scale = 2)
    private BigDecimal exposureLimitCny;

    @Column(name = "exposure_ratio", nullable = false, precision = 10, scale = 6)
    private BigDecimal exposureRatio;

    @Column(name = "is_circuit_breaker_triggered", nullable = false)
    private Boolean isCircuitBreakerTriggered;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
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

    public Boolean getIsCircuitBreakerTriggered() {
        return isCircuitBreakerTriggered;
    }

    public void setIsCircuitBreakerTriggered(Boolean isCircuitBreakerTriggered) {
        this.isCircuitBreakerTriggered = isCircuitBreakerTriggered;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
