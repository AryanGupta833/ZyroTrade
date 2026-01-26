package com.ZyroTrade.Price;

import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Transactional
public class PriceService {

    private final StockRepository stockRepository;
    private final FinnhubClient finnhubClient;

    public PriceService(StockRepository stockRepository,
                        FinnhubClient finnhubClient) {
        this.stockRepository = stockRepository;
        this.finnhubClient = finnhubClient;
    }

    public double getPrice(String symbol) {

        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new RuntimeException("Stock not found"));

        try {
            double livePrice = finnhubClient.fetchPrice(symbol);

            stock.setPrice(livePrice);
            stock.setLastUpdated(LocalDateTime.now());
            stock.setTradable(true);

            stockRepository.save(stock);
            return livePrice;

        } catch (Exception ex) {

            if (stock.getPrice() > 0) {
                return stock.getPrice(); // fallback to cached
            }

            throw new RuntimeException(
                    "Price unavailable for " + symbol);
        }
    }
}
