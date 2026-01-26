package com.ZyroTrade.config;

import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StockSeeder implements CommandLineRunner {

    @Autowired
    private StockRepository stockRepository;

    @Override
    public void run(String... args) {

        List<Stock> stocks = List.of(
                new Stock(null, "AAPL", "Apple Inc.", 0.0, true, null),
                new Stock(null, "MSFT", "Microsoft Corp.", 0.0, true, null),
                new Stock(null, "GOOGL", "Alphabet Inc.", 0.0, true, null),
                new Stock(null, "AMZN", "Amazon.com Inc.", 0.0, true, null),
                new Stock(null, "TSLA", "Tesla Inc.", 0.0, true, null),
                new Stock(null, "META", "Meta Platforms Inc.", 0.0, true, null),
                new Stock(null, "NVDA", "NVIDIA Corp.", 0.0, true, null),
                new Stock(null, "NFLX", "Netflix Inc.", 0.0, true, null),
                new Stock(null, "INTC", "Intel Corp.", 0.0, true, null),
                new Stock(null, "AMD", "AMD Inc.", 0.0, true, null)
        );

        for (Stock stock : stocks) {
            if (!stockRepository.existsBySymbol(stock.getSymbol())) {
                stockRepository.save(stock);
                System.out.println("Seeded: " + stock.getSymbol());
            }
        }

        System.out.println("ZyroTrade stock seeding completed safely");
    }
}
