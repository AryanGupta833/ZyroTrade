package com.ZyroTrade.PriceAlert;

import com.ZyroTrade.Notification.NotificationService;
import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class PriceAlertScheduler {
    private final PriceAlertRepository alertRepository;
    private final StockRepository stockRepository;
    private final NotificationService notificationService;
    private final long maxQuoteAgeSeconds;

    public PriceAlertScheduler(PriceAlertRepository alertRepository, StockRepository stockRepository,
                               NotificationService notificationService,
                               @Value("${trading.max-price-age-seconds:120}") long maxQuoteAgeSeconds) {
        this.alertRepository = alertRepository;
        this.stockRepository = stockRepository;
        this.notificationService = notificationService;
        this.maxQuoteAgeSeconds = maxQuoteAgeSeconds;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkPriceAlerts() {
        for (PriceAlert alert : alertRepository.findUntriggeredForUpdate()) {
            Stock stock = stockRepository.findBySymbol(alert.getSymbol()).orElse(null);
            if (stock == null || !isFresh(stock.getLastUpdated()) || stock.getPrice() == null
                    || stock.getPrice().signum() <= 0 || alert.getTargetPrice() == null || alert.getAlertType() == null) {
                continue;
            }
            int comparison = stock.getPrice().compareTo(alert.getTargetPrice());
            boolean hit = alert.getAlertType() == AlertType.ABOVE ? comparison >= 0 : comparison <= 0;
            if (hit) {
                notificationService.notify(alert.getUsername(), "Price Alert Triggered",
                        alert.getSymbol() + " crossed ₹" + alert.getTargetPrice());
                alert.setTriggered(true);
            }
        }
    }

    private boolean isFresh(LocalDateTime updated) {
        return maxQuoteAgeSeconds >= 0 && updated != null && !updated.isAfter(LocalDateTime.now())
                && !updated.isBefore(LocalDateTime.now().minus(Duration.ofSeconds(maxQuoteAgeSeconds)));
    }
}
