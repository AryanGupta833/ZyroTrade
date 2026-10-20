package com.ZyroTrade.ledger;

import java.math.BigDecimal;
import java.util.List;

public record WalletReconciliationResult(
        Long walletId,
        String username,
        String currency,
        BigDecimal walletBalance,
        BigDecimal ledgerBalance,
        BigDecimal discrepancy,
        boolean hasOpeningEntry,
        long orderCount,
        long orderPostingCount,
        List<Long> missingOrderIds,
        boolean reconciled
) {
}
