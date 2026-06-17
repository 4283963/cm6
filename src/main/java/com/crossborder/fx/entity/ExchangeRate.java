package com.crossborder.fx.entity;

import com.crossborder.fx.common.Currency;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fx_rate")
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_currency", nullable = false)
    private Currency baseCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "quote_currency", nullable = false)
    private Currency quoteCurrency;

    @Column(name = "buy_price", nullable = false, precision = 18, scale = 6)
    private BigDecimal buyPrice;

    @Column(name = "sell_price", nullable = false, precision = 18, scale = 6)
    private BigDecimal sellPrice;

    @Column(name = "mid_price", nullable = false, precision = 18, scale = 6)
    private BigDecimal midPrice;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Currency getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(Currency baseCurrency) { this.baseCurrency = baseCurrency; }

    public Currency getQuoteCurrency() { return quoteCurrency; }
    public void setQuoteCurrency(Currency quoteCurrency) { this.quoteCurrency = quoteCurrency; }

    public BigDecimal getBuyPrice() { return buyPrice; }
    public void setBuyPrice(BigDecimal buyPrice) { this.buyPrice = buyPrice; }

    public BigDecimal getSellPrice() { return sellPrice; }
    public void setSellPrice(BigDecimal sellPrice) { this.sellPrice = sellPrice; }

    public BigDecimal getMidPrice() { return midPrice; }
    public void setMidPrice(BigDecimal midPrice) { this.midPrice = midPrice; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
