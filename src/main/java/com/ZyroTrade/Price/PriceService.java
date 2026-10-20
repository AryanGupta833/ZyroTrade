package com.ZyroTrade.Price;

import com.ZyroTrade.Stocks.StockRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class PriceService {
    private final StockRepository stockRepository;
    private final FinnhubClient finnhubClient;
    private final StockQuoteUpdateService quoteUpdateService;
    private final long maxPriceAgeSeconds;

    public PriceService(StockRepository stockRepository, FinnhubClient finnhubClient,
                        StockQuoteUpdateService quoteUpdateService,
                        @Value("${trading.max-price-age-seconds:120}") long maxPriceAgeSeconds) {
        this.stockRepository = stockRepository;
        this.finnhubClient = finnhubClient;
        this.quoteUpdateService = quoteUpdateService;
        this.maxPriceAgeSeconds = maxPriceAgeSeconds;
    }

    public BigDecimal getPrice(String symbol) {
        if (stockRepository.findBySymbol(symbol).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found");
        }
        var quote = finnhubClient.fetchQuote(symbol);
        Instant instant = Instant.ofEpochSecond(quote.getQuoteEpochSeconds());
        Instant now = Instant.now();
        if (maxPriceAgeSeconds < 0 || instant.isAfter(now)
                || instant.isBefore(now.minusSeconds(maxPriceAgeSeconds))) {
            throw new MarketDataUnavailableException();
        }
        quoteUpdateService.updateIfNewer(symbol, quote.getCurrentPrice(),
                LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
        return stockRepository.findBySymbol(symbol).orElseThrow(() -> new MarketDataUnavailableException()).getPrice();
    }
}
