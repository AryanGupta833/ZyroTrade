package com.ZyroTrade.ledger;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ledger_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uq_wallet_ledger_currency", columnNames = {"wallet_id", "currency"}),
        indexes = @Index(name = "ix_ledger_account_wallet", columnList = "wallet_id"))
@Data
@NoArgsConstructor
public class LedgerAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_code", nullable = false, unique = true, length = 96)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 16)
    private LedgerAccountType type;

    @Column(nullable = false, length = 3)
    private String currency;

    // Intentionally not a foreign key so ledger accounts survive wallet archival.
    @Column(name = "wallet_id")
    private Long walletId;

    public static LedgerAccount cashAsset(String currency) {
        LedgerAccount account = new LedgerAccount();
        account.setCode("CASH_ASSET_" + currency);
        account.setName("Cash settlement asset (" + currency + ")");
        account.setType(LedgerAccountType.ASSET);
        account.setCurrency(currency);
        return account;
    }

    public static LedgerAccount walletLiability(Long walletId, String currency) {
        LedgerAccount account = new LedgerAccount();
        account.setCode("WALLET_CASH_" + walletId + "_" + currency);
        account.setName("Customer wallet cash payable (" + currency + ")");
        account.setType(LedgerAccountType.LIABILITY);
        account.setCurrency(currency);
        account.setWalletId(walletId);
        return account;
    }
}
