package com.ZyroTrade.Price;


import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.chrono.ChronoLocalDateTime;

@Component
public class PriceScheduler {
    @Autowired
    private ExternalPriceService externalPriceService;
    @Autowired
    private StockRepository stockRepository;

    @Scheduled(fixedDelay = 60000)
    public void updatePriceDuringMarketHours(){
        System.out.println("Scheduler ticked");
        externalPriceService.refreshPrices();
    }

}
