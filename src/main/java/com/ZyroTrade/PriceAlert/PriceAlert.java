package com.ZyroTrade.PriceAlert;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "price_alerts")
public class PriceAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String symbol;

    private double targetPrice;

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
