package com.ZyroTrade.ledger;

import com.ZyroTrade.Wallet.Wallet;
import com.ZyroTrade.Wallet.WalletRepository;
import com.ZyroTrade.Orders.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LedgerReconciliationService {
    private final WalletRepository walletRepository;
    private final LedgerAccountRepository accountRepository;
    private final LedgerLineRepository lineRepository;
    private final LedgerService ledgerService;
    private final JournalEntryRepository journalRepository;
    private final OrderRepository orderRepository;

    public LedgerReconciliationService(WalletRepository walletRepository,
                                       LedgerAccountRepository accountRepository,
                                       LedgerLineRepository lineRepository,
                                       LedgerService ledgerService,
                                       JournalEntryRepository journalRepository,
                                       OrderRepository orderRepository) {
        this.walletRepository = walletRepository;
        this.accountRepository = accountRepository;
        this.lineRepository = lineRepository;
        this.ledgerService = ledgerService;
        this.journalRepository = journalRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<WalletReconciliationResult> reconcileAllWallets() {
        return walletRepository.findAll().stream().map(this::reconcile).toList();
    }

    @Transactional(readOnly = true)
    public WalletReconciliationResult reconcile(Wallet wallet) {
        var account = accountRepository.findByWalletIdAndCurrency(wallet.getId(), ledgerService.getCurrency());
        BigDecimal ledgerBalance = account.map(value -> lineRepository.sumCreditNormalBalance(value.getId())).orElse(BigDecimal.ZERO);
        if (ledgerBalance == null) ledgerBalance = BigDecimal.ZERO;
        BigDecimal discrepancy = wallet.getBalance().subtract(ledgerBalance);
        String openingReference = "WALLET_OPENING:" + wallet.getId();
        String migrationReference = "MIGRATION_OPENING:" + wallet.getId();
        boolean hasOpeningEntry = journalRepository.findByBusinessReference(openingReference).isPresent()
                || journalRepository.findByBusinessReference(migrationReference).isPresent();
        long orderCount = orderRepository.countByUserUsername(wallet.getUser().getUsername());
        List<Long> missingOrderIds = orderRepository.findMissingLedgerOrderIds(wallet.getUser().getUsername());
        long orderPostingCount = account.map(value -> journalRepository.countOrderPostingsForWalletAccount(value.getId())).orElse(0L);
        return new WalletReconciliationResult(wallet.getId(), wallet.getUser().getUsername(), ledgerService.getCurrency(),
                wallet.getBalance(), ledgerBalance, discrepancy, hasOpeningEntry, orderCount, orderPostingCount,
                missingOrderIds, discrepancy.signum() == 0 && hasOpeningEntry && missingOrderIds.isEmpty()
                && orderCount == orderPostingCount);
    }
}
