package com.ZyroTrade.dailyPnlSnapshot;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "daily_pnl_snapshots",uniqueConstraints = @UniqueConstraint(columnNames = {"username","snapshotDate"}))
public class DailyPnlSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private LocalDate snapshotDate;
    private double realizedPnl;
    private double unrealizedPnl;
    private double netPnl;
    private double walletBalance;
    private double portfolioValue;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate(){
        createdAt = LocalDateTime.now();
        if (snapshotDate == null) {
            snapshotDate = LocalDate.now();
        }
    }

}
