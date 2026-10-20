package com.ZyroTrade.ledger;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entries", uniqueConstraints =
        @UniqueConstraint(name = "uq_journal_business_reference", columnNames = "business_reference"),
        indexes = @Index(name = "ix_journal_created_at", columnList = "created_at"))
@Data
@NoArgsConstructor
public class JournalEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_reference", nullable = false, length = 128)
    private String businessReference;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private JournalStatus status = JournalStatus.POSTED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LedgerLine> lines = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id", unique = true)
    private JournalEntry reversalOf;

    void addLine(LedgerLine line) {
        line.setJournalEntry(this);
        lines.add(line);
    }

    @PrePersist
    void validatePostedEntry() {
        if (status != JournalStatus.POSTED || lines.size() < 2) {
            throw new IllegalStateException("A posted journal requires at least two lines");
        }
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        for (LedgerLine line : lines) {
            if (line.getAccount() == null || !currency.equals(line.getAccount().getCurrency())
                    || line.getAmount() == null || line.getAmount().signum() <= 0) {
                throw new IllegalStateException("Journal lines require positive amounts in the journal currency");
            }
            if (line.getSide() == LedgerSide.DEBIT) debits = debits.add(line.getAmount());
            else if (line.getSide() == LedgerSide.CREDIT) credits = credits.add(line.getAmount());
            else throw new IllegalStateException("Journal lines require a debit or credit side");
        }
        if (debits.compareTo(credits) != 0) throw new IllegalStateException("Journal debits and credits must balance");
    }

    @PreUpdate
    void rejectUpdate() {
        throw new IllegalStateException("Journal entries are immutable");
    }

    @PreRemove
    void rejectDelete() {
        throw new IllegalStateException("Journal entries cannot be deleted");
    }
}
