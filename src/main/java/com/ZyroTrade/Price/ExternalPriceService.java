package com.ZyroTrade.Price;

import com.ZyroTrade.DTO.FinhubQuoteResponse;
import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ExternalPriceService {

    @Value("${finhub.api.key}")
    private String apiKey;

    @Autowired
    private StockRepository stockRepository;

    private final RestTemplate restTemplate = new RestTemplate();


    private final AtomicInteger rrIndex = new AtomicInteger(0);

    public void refreshPrices() {
        System.out.println("Prices refreshed (Finnhub Round Robin)");

        List<Stock> stocks = stockRepository.findAll();
        if (stocks.isEmpty()) return;

        int startIndex = rrIndex.getAndUpdate(i -> (i + 1) % stocks.size());
        Stock stock = stocks.get(startIndex);

        try {
            String url = "https://finnhub.io/api/v1/quote" +
                    "?symbol=" + stock.getSymbol() +
                    "&token=" + apiKey;

            System.out.println("RR Fetching price for " + stock.getSymbol());

            FinhubQuoteResponse response =
                    restTemplate.getForObject(url, FinhubQuoteResponse.class);

            if (response == null || response.getCurrentPrice() == null || response.getCurrentPrice() <= 0) {
                System.out.println("Finnhub missing/invalid for " + stock.getSymbol());
                return;
            }

            stock.setPrice(response.getCurrentPrice());
            stock.setLastUpdated(LocalDateTime.now());
            stock.setTradable(true);
            stockRepository.save(stock);

            System.out.println("Updated " + stock.getSymbol() + " -> " + response.getCurrentPrice());

        } catch (Exception e) {
            System.out.println("Finnhub error for " + stock.getSymbol() + ": " + e.getMessage());
        }
    }
}
