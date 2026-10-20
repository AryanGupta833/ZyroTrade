package com.ZyroTrade.ledger;

import com.ZyroTrade.Orders.Order;
import com.ZyroTrade.Orders.OrderType;
import com.ZyroTrade.Wallet.Wallet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class LedgerService {
    public static final String CASH_ASSET_PREFIX = "CASH_ASSET_";
    private final LedgerAccountRepository accountRepository;
    private final JournalEntryRepository journalRepository;
    private final LedgerLineRepository lineRepository;
    private final String currency;

    public LedgerService(LedgerAccountRepository accountRepository,
                         JournalEntryRepository journalRepository,
                         LedgerLineRepository lineRepository,
                         @Value("${ledger.currency:INR}") String currency) {
        this.accountRepository = accountRepository;
        this.journalRepository = journalRepository;
        this.lineRepository = lineRepository;
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }

    @Transactional
    public void ensureCashAssetAccount() {
        accountRepository.findByCode(CASH_ASSET_PREFIX + currency)
                .orElseGet(() -> accountRepository.saveAndFlush(LedgerAccount.cashAsset(currency)));
    }

    @Transactional
    public JournalEntry postOpeningWalletBalance(Wallet wallet, BigDecimal amount, String reference) {
        BigDecimal normalized = normalize(amount);
        String openingReference = "WALLET_OPENING:" + wallet.getId();
        String migrationReference = "MIGRATION_OPENING:" + wallet.getId();
        if (!openingReference.equals(reference) && !migrationReference.equals(reference)) {
            throw new IllegalArgumentException("Opening reference must identify the wallet and opening purpose");
        }
        LedgerAccount cash = requireCashAsset();
        LedgerAccount payable = walletAccount(wallet);
        var prior = journalRepository.findByBusinessReference(reference);
        if (prior.isPresent()) {
            List<LedgerLine> lines = lineRepository.findByJournalEntryIdOrderByLineNumber(prior.get().getId());
            boolean same = prior.get().getCurrency().equals(currency) && lines.size() == 2
                    && lines.get(0).getAccount().getId().equals(cash.getId())
                    && lines.get(0).getSide() == LedgerSide.DEBIT
                    && lines.get(0).getAmount().compareTo(normalized) == 0
                    && lines.get(1).getAccount().getId().equals(payable.getId())
                    && lines.get(1).getSide() == LedgerSide.CREDIT
                    && lines.get(1).getAmount().compareTo(normalized) == 0;
            if (same) return prior.get();
            throw new IllegalStateException("Opening reference already exists with different financial details");
        }
        if (normalized.signum() == 0) return null;
        if (!lineRepository.findByAccountId(payable.getId()).isEmpty()) {
            throw new IllegalStateException("Wallet already has ledger postings");
        }
        return post(reference, "Initial wallet funding", currency, List.of(
                new LedgerPosting(cash.getCode(), LedgerSide.DEBIT, normalized),
                new LedgerPosting(payable.getCode(), LedgerSide.CREDIT, normalized)
        ));
    }

    @Transactional
    public JournalEntry postOrder(Order order, Wallet wallet) {
        BigDecimal amount = normalize(order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity())));
        LedgerAccount cash = requireCashAsset();
        LedgerAccount payable = walletAccount(wallet);
        boolean hasAuditedOpening = lineRepository.existsByAccountIdAndJournalEntryBusinessReference(
                payable.getId(), "WALLET_OPENING:" + wallet.getId())
                || lineRepository.existsByAccountIdAndJournalEntryBusinessReference(
                payable.getId(), "MIGRATION_OPENING:" + wallet.getId());
        if (!hasAuditedOpening) {
            throw new LedgerCutoverRequiredException();
        }
        List<LedgerPosting> postings = order.getType() == OrderType.BUY
                ? List.of(new LedgerPosting(payable.getCode(), LedgerSide.DEBIT, amount),
                          new LedgerPosting(cash.getCode(), LedgerSide.CREDIT, amount))
                : List.of(new LedgerPosting(cash.getCode(), LedgerSide.DEBIT, amount),
                          new LedgerPosting(payable.getCode(), LedgerSide.CREDIT, amount));
        return post("ORDER:" + order.getId(), "Order " + order.getType() + " " + order.getId(), currency, postings);
    }

    @Transactional
    public JournalEntry post(String reference, String description, String entryCurrency, List<LedgerPosting> postings) {
        return postInternal(reference, description, entryCurrency, postings, null);
    }

    @Transactional
    public JournalEntry postCompensatingEntry(String originalReference, String compensationReference, String reason) {
        JournalEntry original = journalRepository.findByBusinessReference(originalReference)
                .orElseThrow(() -> new IllegalArgumentException("Original journal entry not found"));
        if (journalRepository.existsByReversalOfId(original.getId())) {
            throw new IllegalArgumentException("Journal entry already has a compensating entry");
        }
        List<LedgerPosting> reversal = lineRepository.findByJournalEntryIdOrderByLineNumber(original.getId()).stream()
                .map(line -> new LedgerPosting(line.getAccount().getCode(),
                        line.getSide() == LedgerSide.DEBIT ? LedgerSide.CREDIT : LedgerSide.DEBIT,
                        line.getAmount()))
                .toList();
        return postInternal(compensationReference, "Compensates " + originalReference + ": " + reason,
                original.getCurrency(), reversal, original);
    }

    private JournalEntry postInternal(String reference, String description, String entryCurrency,
                                      List<LedgerPosting> postings, JournalEntry reversalOf) {
        if (reference == null || reference.isBlank() || reference.length() > 128) {
            throw new IllegalArgumentException("A business reference of 1 to 128 characters is required");
        }
        if (journalRepository.findByBusinessReference(reference).isPresent()) {
            throw new IllegalArgumentException("A journal entry already exists for business reference " + reference);
        }
        if (postings == null || postings.size() < 2) throw new IllegalArgumentException("A journal requires at least two lines");

        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        List<LedgerAccount> accounts = postings.stream().map(posting -> {
            if (posting == null || posting.side() == null || posting.accountCode() == null) {
                throw new IllegalArgumentException("Every journal line requires an account and side");
            }
            BigDecimal amount = normalize(posting.amount());
            if (amount.signum() <= 0) throw new IllegalArgumentException("Ledger amounts must be positive");
            return accountRepository.findByCode(posting.accountCode())
                    .orElseThrow(() -> new IllegalArgumentException("Ledger account not found: " + posting.accountCode()));
        }).toList();
        for (int i = 0; i < postings.size(); i++) {
            LedgerPosting posting = postings.get(i);
            BigDecimal amount = normalize(posting.amount());
            LedgerAccount account = accounts.get(i);
            if (!account.getCurrency().equals(entryCurrency)) {
                throw new IllegalArgumentException("All journal accounts must use the entry currency");
            }
            if (posting.side() == LedgerSide.DEBIT) debits = debits.add(amount);
            else credits = credits.add(amount);
        }
        if (debits.compareTo(credits) != 0) throw new IllegalArgumentException("Journal debits and credits must balance");

        JournalEntry entry = new JournalEntry();
        entry.setBusinessReference(reference);
        entry.setDescription(description);
        entry.setCurrency(entryCurrency);
        entry.setStatus(JournalStatus.POSTED);
        entry.setReversalOf(reversalOf);
        for (int i = 0; i < postings.size(); i++) {
            LedgerPosting posting = postings.get(i);
            LedgerLine line = new LedgerLine();
            line.setAccount(accounts.get(i));
            line.setLineNumber(i + 1);
            line.setSide(posting.side());
            line.setAmount(normalize(posting.amount()));
            entry.addLine(line);
        }
        return journalRepository.saveAndFlush(entry);
    }

    @Transactional(readOnly = true)
    public List<LedgerLine> getLines(Long journalEntryId) {
        return lineRepository.findByJournalEntryIdOrderByLineNumber(journalEntryId);
    }

    @Transactional
    public LedgerAccount walletAccount(Wallet wallet) {
        return accountRepository.findByWalletIdAndCurrency(wallet.getId(), currency)
                .orElseGet(() -> accountRepository.saveAndFlush(LedgerAccount.walletLiability(wallet.getId(), currency)));
    }

    private LedgerAccount requireCashAsset() {
        return accountRepository.findByCode(CASH_ASSET_PREFIX + currency)
                .orElseThrow(() -> new IllegalStateException("Ledger cash asset account is not initialized"));
    }

    private BigDecimal normalize(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Ledger amount is required");
        return amount.setScale(4, RoundingMode.HALF_EVEN);
    }
}
