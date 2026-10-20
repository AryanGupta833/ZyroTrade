package com.ZyroTrade.Stocks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class StockService {

    @Autowired
    private StockRepository stockRepository;
    @Value("${trading.max-price-age-seconds:120}")
    private long maxQuoteAgeSeconds;

    public List<Stock> getAllStocks(){
        return stockRepository.findAll();
    }
    public Page<Stock> search(String query, Pageable pageable) {
        if (query == null || query.isBlank() || query.length() > 64) {
            throw new IllegalArgumentException("Search query must contain 1 to 64 characters");
        }
        return stockRepository.findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase(query.trim(), query.trim(), pageable);
    }
    public Stock addStock(Stock stock){
        stockRepository.save(stock);
        return stock;
    }
    public Stock updatePrice(Long id, BigDecimal price){
        if (price == null || price.signum() <= 0) throw new IllegalArgumentException("Price must be positive");
        Stock stock=stockRepository.findById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found"));
        stock.setPrice(price);
        return stockRepository.save(stock);
    }
    public void deleteStock(Long id){
        stockRepository.deleteById(id);
    }

    public Stock setTradable(Long id, boolean b) {
        Stock stock=stockRepository.findById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found"));
        if (b && (stock.getPrice() == null || stock.getPrice().signum() <= 0 || stock.getLastUpdated() == null
                || stock.getLastUpdated().isAfter(LocalDateTime.now())
                || maxQuoteAgeSeconds < 0 || stock.getLastUpdated().isBefore(LocalDateTime.now().minusSeconds(maxQuoteAgeSeconds)))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A fresh valid quote is required to resume trading");
        }
        stock.setTradable(b);
        return stockRepository.save(stock);

    }
}
