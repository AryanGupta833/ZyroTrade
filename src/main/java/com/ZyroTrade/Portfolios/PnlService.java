package com.ZyroTrade.Portfolios;

import com.ZyroTrade.DTO.PnlResponse;
import com.ZyroTrade.Orders.Order;
import com.ZyroTrade.Orders.OrderRepository;
import com.ZyroTrade.Orders.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import com.ZyroTrade.Price.QuoteFreshnessStatus;

@Service
public class PnlService {
  @Autowired
  private PortfolioRepository portfolioRepository;

  @Autowired
  private OrderRepository orderRepository;
  @Value("${trading.max-price-age-seconds:120}")
  private long maxPriceAgeSeconds;

  public List<PnlResponse> unrealizedPnl(String username)
  {
      List<Portfolio> portfolios=portfolioRepository.findByUserUsername(username);
      return portfolios.stream()
              .map(p -> {
                  BigDecimal currentPrice = p.getStock().getPrice();
                  BigDecimal avgPrice = p.getAvgPrice();
                  BigDecimal costBasis = p.getCostBasis();
                  boolean basisAvailable = costBasis != null && costBasis.signum() > 0;
                  BigDecimal pnl = basisAvailable ? currentPrice.multiply(BigDecimal.valueOf(p.getQuantity())).subtract(costBasis)
                          .setScale(4, RoundingMode.HALF_EVEN) : null;

                  return new PnlResponse(
                          p.getStock().getSymbol(),
                          p.getQuantity(),
                          p.getAvgPrice(),
                          currentPrice,
                          pnl,
                          QuoteFreshnessStatus.evaluate(currentPrice, p.getStock().getLastUpdated(), maxPriceAgeSeconds).name(),
                          basisAvailable ? "REVIEWED" : "REVIEW_REQUIRED"
                  );
              })
              .toList();
  }

    public BigDecimal realizedPnl(String username) {

        List<Order> sellOrders =
                orderRepository.findByUserUsernameAndType(
                        username, OrderType.SELL);

        return sellOrders.stream().map(o -> o.getRealizedPnl() == null ? BigDecimal.ZERO : o.getRealizedPnl())
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_EVEN);
    }

}
