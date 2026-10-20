package com.ZyroTrade.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {
    Optional<LedgerAccount> findByCode(String code);
    Optional<LedgerAccount> findByWalletIdAndCurrency(Long walletId, String currency);
    List<LedgerAccount> findByWalletId(Long walletId);
}
