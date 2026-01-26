package com.ZyroTrade.Portfolios;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio,Long> {
    Optional<Portfolio> findByUserUsernameAndStockId(String username, Long stockId);
    List<Portfolio> findByUserUsername(String username);

    Optional<Portfolio> findByUserIdAndStockSymbol(Long userId, String symbol);

}
