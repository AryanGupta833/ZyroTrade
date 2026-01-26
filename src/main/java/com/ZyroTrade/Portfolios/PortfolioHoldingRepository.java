package com.ZyroTrade.Portfolios;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHolding,Long> {

    List<PortfolioHolding> findByUserid(Long userid);

    boolean existsByUseridAndSymbol(Long userid, String symbol);
}
