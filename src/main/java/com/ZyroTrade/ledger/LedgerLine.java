package com.ZyroTrade.ledger;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "ledger_lines", uniqueConstraints =
        @UniqueConstraint(name = "uq_ledger_entry_line_number", columnNames = {"journal_entry_id", "line_number"}),
        indexes = {
                @Index(name = "ix_ledger_line_account", columnList = "account_id"),
                @Index(name = "ix_ledger_line_journal", columnList = "journal_entry_id")
        })
@Data
@NoArgsConstructor
public class LedgerLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private LedgerAccount account;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private LedgerSide side;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @PreUpdate
    @PreRemove
    void rejectMutation() {
        throw new IllegalStateException("Ledger lines are immutable");
    }
}
