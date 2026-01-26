package com.ZyroTrade.WalletTransaction;

import com.ZyroTrade.Wallet.Wallet;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

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

    private double amount;
    private double balanceAfter;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate(){
        createdAt=LocalDateTime.now();
    }


}
