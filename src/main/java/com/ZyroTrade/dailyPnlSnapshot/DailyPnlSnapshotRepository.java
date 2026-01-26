package com.ZyroTrade.dailyPnlSnapshot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPnlSnapshotRepository extends JpaRepository<DailyPnlSnapshot,Long> {
    List<DailyPnlSnapshot> findByUsernameOrderBySnapshotDateDesc(String username);
    Optional<DailyPnlSnapshot> findByUsernameAndSnapshotDate(
            String username, LocalDate snapshotDate);
    boolean existsByUsernameAndSnapshotDate(
            String username, LocalDate snapshotDate);
    List<DailyPnlSnapshot>
    findByUsernameAndSnapshotDateAfterOrderBySnapshotDateAsc(
            String username, LocalDate fromDate);
    List<DailyPnlSnapshot>
    findByUsernameAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            String username, LocalDate fromDate, LocalDate toDate);
}
