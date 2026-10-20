package com.ZyroTrade.Stocks;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface StockRepository extends JpaRepository<Stock,Long> {
     Optional<Stock> findBySymbol(String symbol);
    Page<Stock> findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase(String symbol, String name, Pageable pageable);
    boolean existsBySymbol(String symbol);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.symbol = :symbol")
    Optional<Stock> findBySymbolForUpdate(@Param("symbol") String symbol);
}
