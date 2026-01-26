package com.ZyroTrade.Portfolios;

import com.ZyroTrade.DTO.HoldingPnlResponse;
import com.ZyroTrade.DTO.PortfolioPnlResponse;
import com.ZyroTrade.Orders.Order;
import com.ZyroTrade.Orders.OrderRepository;
import com.ZyroTrade.Orders.OrderType;
import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.user.User;
import com.ZyroTrade.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PortfolioPnlService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    public PortfolioPnlResponse calculatePnlByUsername(String username){

        User user = userRepository
                .findByUsername(username)
                .orElseThrow();

        List<Order> orders = orderRepository.findByUserUsernameOrderByCreatedAtDesc(username);

        Map<Long, HoldingAccumulator> map = new HashMap<>();

        for (Order o : orders) {
            Long stockId = o.getStock().getId();

            HoldingAccumulator acc =
                    map.getOrDefault(stockId, new HoldingAccumulator());

            if (o.getType() == OrderType.BUY) {
                acc.quantity += o.getQuantity();
                acc.buyQty += o.getQuantity();
                acc.totalBuyValue += o.getPrice() * o.getQuantity();
            } else if (o.getType() == OrderType.SELL) {
                acc.quantity -= o.getQuantity();
            }

            acc.stock = o.getStock();
            map.put(stockId, acc);
        }

        List<HoldingPnlResponse> responses = new ArrayList<>();
        double totalPnl = 0;

        for (HoldingAccumulator acc : map.values()) {

            if (acc.quantity <= 0) continue;

            double avgBuy = acc.totalBuyValue / acc.buyQty;
            double currentPrice = acc.stock.getPrice();

            double pnl = (currentPrice - avgBuy) * acc.quantity;
            totalPnl += pnl;

            responses.add(
                    new HoldingPnlResponse(
                            acc.stock.getSymbol(),
                            acc.quantity,
                            avgBuy,
                            currentPrice,
                            pnl
                    )
            );
        }

        return new PortfolioPnlResponse(responses, totalPnl);
    }


    static class HoldingAccumulator {
        int quantity = 0;
        int buyQty = 0;
        double totalBuyValue = 0;
        Stock stock;
    }
}
