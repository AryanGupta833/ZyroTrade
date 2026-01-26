package com.ZyroTrade.WalletTransaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction,Long> {
    List<WalletTransaction> findByWalletUserUsernameOrderByCreatedAtDesc(String username);
}
