package com.ZyroTrade.Portfolios;

import com.ZyroTrade.DTO.PnlResponse;
import com.ZyroTrade.Orders.Order;
import com.ZyroTrade.Orders.OrderRepository;
import com.ZyroTrade.Orders.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PnlService {
  @Autowired
  private PortfolioRepository portfolioRepository;

  @Autowired
  private OrderRepository orderRepository;

  public List<PnlResponse> unrealizedPnl(String username)
  {
      List<Portfolio> portfolios=portfolioRepository.findByUserUsername(username);
      return portfolios.stream()
              .map(p -> {
                  double pnl =
                          (p.getStock().getPrice() - p.getAvgPrice())
                                  * p.getQuantity();

                  return new PnlResponse(
                          p.getStock().getSymbol(),
                          p.getQuantity(),
                          p.getAvgPrice(),
                          p.getStock().getPrice(),
                          pnl
                  );
              })
              .toList();
  }

    public double realizedPnl(String username) {

        List<Order> sellOrders =
                orderRepository.findByUserUsernameAndType(
                        username, OrderType.SELL);

        return sellOrders.stream()
                .mapToDouble(o -> o.getPrice() * o.getQuantity())
                .sum();
    }

}
