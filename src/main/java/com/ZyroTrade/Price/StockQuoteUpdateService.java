package com.ZyroTrade.Price;

import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class StockQuoteUpdateService {
    private final StockRepository stocks;

    public StockQuoteUpdateService(StockRepository stocks) {
        this.stocks = stocks;
    }

    @Transactional
    public boolean updateIfNewer(String symbol, BigDecimal price, LocalDateTime quoteTime) {
        Stock stock = stocks.findBySymbolForUpdate(symbol).orElse(null);
        if (stock == null || (stock.getLastUpdated() != null && !quoteTime.isAfter(stock.getLastUpdated()))) return false;
        stock.setPrice(price);
        stock.setLastUpdated(quoteTime);
        stocks.save(stock);
        return true;
    }
}
