package com.ZyroTrade.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface LedgerLineRepository extends JpaRepository<LedgerLine, Long> {
    List<LedgerLine> findByJournalEntryIdOrderByLineNumber(Long journalEntryId);
    List<LedgerLine> findByAccountId(Long accountId);
    boolean existsByAccountIdAndJournalEntryBusinessReference(Long accountId, String businessReference);

    @Query(value = "select coalesce(sum(case when side = 'CREDIT' then amount else -amount end), 0) " +
            "from ledger_lines where account_id = :accountId", nativeQuery = true)
    BigDecimal sumCreditNormalBalance(@Param("accountId") Long accountId);
}
