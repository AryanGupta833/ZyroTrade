package com.ZyroTrade.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Optional<JournalEntry> findByBusinessReference(String businessReference);
    boolean existsByBusinessReference(String businessReference);
    boolean existsByReversalOfId(Long originalEntryId);
    @Query("select count(distinct j.id) from JournalEntry j join j.lines l where l.account.id = :accountId and j.businessReference like 'ORDER:%'")
    long countOrderPostingsForWalletAccount(@Param("accountId") Long accountId);
}
