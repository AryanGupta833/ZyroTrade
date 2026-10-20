package com.ZyroTrade.config;

import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.math.BigDecimal;

@Component
public class StockSeeder implements CommandLineRunner {

    @Autowired
    private StockRepository stockRepository;

    @Override
    public void run(String... args) {

        List<Stock> stocks = List.of(
                new Stock(null, "AAPL", "Apple Inc.", BigDecimal.ZERO.setScale(4), true, null),
                new Stock(null, "MSFT", "Microsoft Corp.", BigDecimal.ZERO.setScale(4), true, null),
                new Stock(null, "GOOGL", "Alphabet Inc.", BigDecimal.ZERO.setScale(4), true, null),
                new Stock(null, "AMZN", "Amazon.com Inc.", BigDecimal.ZERO.setScale(4), true, null),
                new Stock(null, "TSLA", "Tesla Inc.", BigDecimal.ZERO.setScale(4), true, null),
                new Stock(null, "META", "Meta Platforms Inc.", BigDecimal.ZERO.setScale(4), true, null),
                new Stock(null, "NVDA", "NVIDIA Corp.", BigDecimal.ZERO.setScale(4), true, null),
                new Stock(null, "NFLX", "Netflix Inc.", BigDecimal.ZERO.setScale(4), true, null),
                new Stock(null, "INTC", "Intel Corp.", BigDecimal.ZERO.setScale(4), true, null),
                new Stock(null, "AMD", "AMD Inc.", BigDecimal.ZERO.setScale(4), true, null)
        );

        for (Stock stock : stocks) {
            if (!stockRepository.existsBySymbol(stock.getSymbol())) {
                stockRepository.save(stock);
            }
        }

    }
}
