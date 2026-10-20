package com.ZyroTrade.PriceAlert;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Data
@Table(name = "price_alerts")
public class PriceAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String symbol;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal targetPrice;

    public void setTargetPrice(BigDecimal targetPrice) {
        this.targetPrice = targetPrice == null ? null : targetPrice.setScale(4, RoundingMode.HALF_EVEN);
    }

    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    private boolean triggered;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate()
    {
        createdAt=LocalDateTime.now();
        triggered=false;
    }
}
