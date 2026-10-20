package com.ZyroTrade.WalletTransaction;

import com.ZyroTrade.Wallet.Wallet;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Data
@Table(name="wallet_transaction")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "wallet_id",nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    private TransactionType  type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate(){
        createdAt=LocalDateTime.now();
    }


}
