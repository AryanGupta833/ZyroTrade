package com.ZyroTrade.WalletTransaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction,Long> {
    List<WalletTransaction> findByWalletUserUsernameOrderByCreatedAtDesc(String username);
    Page<WalletTransaction> findByWalletUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
}
