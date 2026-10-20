package com.ZyroTrade.dailyPnlSnapshot;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Data
@Table(name = "daily_pnl_snapshots",uniqueConstraints = @UniqueConstraint(columnNames = {"username","snapshotDate"}))
public class DailyPnlSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private LocalDate snapshotDate;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal realizedPnl = BigDecimal.ZERO.setScale(4);
    @Column(precision = 19, scale = 4) private BigDecimal unrealizedPnl = BigDecimal.ZERO.setScale(4);
    @Column(precision = 19, scale = 4) private BigDecimal netPnl = BigDecimal.ZERO.setScale(4);
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal walletBalance = BigDecimal.ZERO.setScale(4);
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal portfolioValue = BigDecimal.ZERO.setScale(4);
    @Column(name = "all_quotes_fresh", nullable = false)
    private boolean allQuotesFresh;
    @Column(name = "all_cost_basis_reviewed", nullable = false)
    private boolean allCostBasisReviewed;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate(){
        createdAt = LocalDateTime.now();
        if (snapshotDate == null) {
            snapshotDate = LocalDate.now();
        }
    }

}
