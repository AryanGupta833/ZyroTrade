package com.ZyroTrade.Portfolios;

import com.ZyroTrade.DTO.HoldingPnlResponse;
import com.ZyroTrade.DTO.PortfolioPnlResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import com.ZyroTrade.Price.QuoteFreshnessStatus;

@Service
public class PortfolioPnlService {
    private final PortfolioRepository portfolioRepository;
    private final long maxPriceAgeSeconds;

    public PortfolioPnlService(PortfolioRepository portfolioRepository,
                               @Value("${trading.max-price-age-seconds:120}") long maxPriceAgeSeconds) {
        this.portfolioRepository = portfolioRepository;
        this.maxPriceAgeSeconds = maxPriceAgeSeconds;
    }

    public PortfolioPnlResponse calculatePnlByUsername(String username) {
        List<HoldingPnlResponse> holdings = portfolioRepository.findByUserUsername(username).stream()
                .filter(p -> p.getQuantity() > 0)
                .map(p -> {
                    BigDecimal average = p.getAvgPrice();
                    BigDecimal costBasis = p.getCostBasis();
                    BigDecimal currentPrice = p.getStock().getPrice();
                    boolean basisAvailable = costBasis != null && costBasis.signum() > 0;
                    BigDecimal pnl = basisAvailable ? currentPrice.multiply(BigDecimal.valueOf(p.getQuantity())).subtract(costBasis)
                            .setScale(4, RoundingMode.HALF_EVEN) : null;
                    String status = QuoteFreshnessStatus.evaluate(currentPrice, p.getStock().getLastUpdated(), maxPriceAgeSeconds).name();
                    return new HoldingPnlResponse(p.getStock().getSymbol(), p.getQuantity(), average, currentPrice, pnl,
                            status, basisAvailable ? "REVIEWED" : "REVIEW_REQUIRED");
                })
                .toList();
        BigDecimal total = holdings.stream().allMatch(h -> h.pnl() != null)
                ? holdings.stream().map(HoldingPnlResponse::pnl).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(4, RoundingMode.HALF_EVEN)
                : null;
        return new PortfolioPnlResponse(holdings, total);
    }
}
