package com.ZyroTrade.DTO;

import com.ZyroTrade.WalletTransaction.WalletTransaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record WalletTransactionResponse(Long id, String type, BigDecimal amount, BigDecimal balanceAfter,
                                        OffsetDateTime createdAt) {
    public static WalletTransactionResponse from(WalletTransaction transaction) {
        return new WalletTransactionResponse(transaction.getId(), transaction.getType().name(), transaction.getAmount(),
                transaction.getBalanceAfter(), transaction.getCreatedAt() == null ? null
                : transaction.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }
}
