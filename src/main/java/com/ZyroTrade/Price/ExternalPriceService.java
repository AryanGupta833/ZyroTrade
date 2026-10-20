package com.ZyroTrade.Price;

import com.ZyroTrade.DTO.FinhubQuoteResponse;
import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import com.ZyroTrade.Stocks.Stock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ExternalPriceService {
    private final StockRepository stockRepository;
    private final FinnhubClient finnhubClient;
    private final MeterRegistry meters;
    private final StockQuoteUpdateService quoteUpdateService;
    private final long maxQuoteAgeSeconds;

    public ExternalPriceService(StockRepository stockRepository, FinnhubClient finnhubClient,
                                StockQuoteUpdateService quoteUpdateService,
                                MeterRegistry meters,
                                @Value("${trading.max-price-age-seconds:120}") long maxQuoteAgeSeconds) {
        this.stockRepository = stockRepository;
        this.finnhubClient = finnhubClient;
        this.quoteUpdateService = quoteUpdateService;
        this.meters = meters;
        this.maxQuoteAgeSeconds = maxQuoteAgeSeconds;
    }

    public void refreshPrices() {
        List<Stock> stocks = stockRepository.findAll();
        if (stocks.isEmpty()) return;
        for (Stock stock : stocks) {
            Timer.Sample sample = Timer.start(meters);
            String outcome = "unavailable";
            try {
                FinhubQuoteResponse quote = finnhubClient.fetchQuote(stock.getSymbol());
                Instant timestamp = Instant.ofEpochSecond(quote.getQuoteEpochSeconds());
                Instant now = Instant.now();
                if (timestamp.isAfter(now)) {
                    outcome = "invalid_timestamp";
                } else if (maxQuoteAgeSeconds < 0 || timestamp.isBefore(now.minusSeconds(maxQuoteAgeSeconds))) {
                    outcome = "stale_quote";
                } else {
                    boolean updated = quoteUpdateService.updateIfNewer(stock.getSymbol(), quote.getCurrentPrice(),
                            LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()));
                    outcome = updated ? "success" : "older_than_stored";
                }
            } catch (MarketDataUnavailableException providerFailure) {
                outcome = "unavailable";
            } finally {
                meters.counter("market_data.refresh", "outcome", outcome).increment();
                sample.stop(meters.timer("market_data.refresh.duration"));
            }
        }
    }
}
