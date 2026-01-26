package com.ZyroTrade.Stocks;

import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.quota.ClientQuotaAlteration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock,Long> {
     Optional<Stock> findBySymbol(String symbol);
    boolean existsBySymbol(String symbol);
}
