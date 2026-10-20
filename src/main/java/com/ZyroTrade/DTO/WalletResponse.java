package com.ZyroTrade.DTO;

import com.ZyroTrade.Wallet.Wallet;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record WalletResponse(Long id, BigDecimal balance, String currency, OffsetDateTime updatedAt) {
    public static WalletResponse from(Wallet wallet, String currency) {
        return new WalletResponse(wallet.getId(), wallet.getBalance(), currency,
                wallet.getUpdatedAt() == null ? null : wallet.getUpdatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }
}
