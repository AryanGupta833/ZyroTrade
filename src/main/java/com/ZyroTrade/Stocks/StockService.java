package com.ZyroTrade.Stocks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockService {

    @Autowired
    private StockRepository stockRepository;

    public List<Stock> getAllStocks(){
        return stockRepository.findAll();
    }
    public Stock addStock(Stock stock){
        stockRepository.save(stock);
        return stock;
    }
    public Stock updatePrice(Long id,double price){
        Stock stock=stockRepository.findById(id).orElseThrow(()->new RuntimeException("Stock not found"));
        stock.setPrice(price);
        return stockRepository.save(stock);
    }
    public void deleteStock(Long id){
        stockRepository.deleteById(id);
    }

    public Stock setTradable(Long id, boolean b) {
        Stock stock=stockRepository.findById(id).orElseThrow(()->new RuntimeException("Stock not found"));
        stock.setTradable(b);
        return stockRepository.save(stock);

    }
}
