package com.ZyroTrade.Orders;

import com.ZyroTrade.Notification.NotificationService;
import com.ZyroTrade.Portfolios.Portfolio;
import com.ZyroTrade.Portfolios.PortfolioRepository;
import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import com.ZyroTrade.Wallet.WalletService;
import com.ZyroTrade.user.User;
import com.ZyroTrade.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class OrderService {

    private final NotificationService notificationService;
    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final WalletService walletService;

    public OrderService(
            NotificationService notificationService,
            OrderRepository orderRepository,
            StockRepository stockRepository,
            UserRepository userRepository,
            PortfolioRepository portfolioRepository,
            WalletService walletService
    ) {
        this.notificationService = notificationService;
        this.orderRepository = orderRepository;
        this.stockRepository = stockRepository;
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.walletService = walletService;
    }

    public Order placeOrder(Long stockId, OrderType type, int quantity) {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new RuntimeException("Stock not found"));

        if (!stock.isTradable()) {
            throw new RuntimeException("Trading is halted for this stock");
        }

        double tradeValue = stock.getPrice() * quantity;

        Portfolio portfolio = portfolioRepository
                .findByUserUsernameAndStockId(username, stockId)
                .orElse(null);


        if (type == OrderType.BUY) {

            walletService.debit(username, tradeValue);

            if (portfolio == null) {
                portfolio = new Portfolio();
                portfolio.setUser(user);
                portfolio.setStock(stock);
                portfolio.setQuantity(0);
                portfolio.setAvgPrice(0);
            }

            int newQty = portfolio.getQuantity() + quantity;

            double newAvgPrice =
                    ((portfolio.getAvgPrice() * portfolio.getQuantity())
                            + (stock.getPrice() * quantity))
                            / newQty;

            portfolio.setQuantity(newQty);
            portfolio.setAvgPrice(newAvgPrice);

            portfolioRepository.save(portfolio);


            Order order = new Order();
            order.setUser(user);
            order.setStock(stock);
            order.setType(type);
            order.setQuantity(quantity);
            order.setPrice(stock.getPrice());
            order.setRealizedPnl(0.0);

            orderRepository.save(order);

            notificationService.notify(
                    username,
                    "Order Executed",
                    type + " " + quantity + " shares of " + stock.getSymbol()
            );

            return order;
        }


        else if (type == OrderType.SELL) {

            if (portfolio == null || portfolio.getQuantity() < quantity) {
                throw new RuntimeException("Not enough quantity to sell");
            }


            double avgBuyPrice = portfolio.getAvgPrice();

            int remainingQty = portfolio.getQuantity() - quantity;

            if (remainingQty == 0) {
                portfolioRepository.delete(portfolio);
            } else {
                portfolio.setQuantity(remainingQty);
                portfolioRepository.save(portfolio);
            }

            walletService.credit(username, tradeValue);


            double realized = (stock.getPrice() - avgBuyPrice) * quantity;

            Order order = new Order();
            order.setUser(user);
            order.setStock(stock);
            order.setType(type);
            order.setQuantity(quantity);
            order.setPrice(stock.getPrice());
            order.setRealizedPnl(realized);

            orderRepository.save(order);

            notificationService.notify(
                    username,
                    "Order Executed",
                    type + " " + quantity + " shares of " + stock.getSymbol()
            );

            return order;
        }

        throw new RuntimeException("Unsupported order type");
    }public List<Order> myOrders() {
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return orderRepository.findByUserUsernameOrderByCreatedAtDesc(username);
    }
}
