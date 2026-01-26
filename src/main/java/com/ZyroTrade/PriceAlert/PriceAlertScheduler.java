package com.ZyroTrade.PriceAlert;


import com.ZyroTrade.Notification.NotificationService;
import com.ZyroTrade.Stocks.Stock;
import com.ZyroTrade.Stocks.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableScheduling
public class PriceAlertScheduler {

    @Autowired
    private PriceAlertRepository priceAlertRepository;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private NotificationService notificationService;

    @Scheduled(fixedDelay = 60000)
    public void checkPriceAlerts(){
        List<PriceAlert> alerts=priceAlertRepository.findByTriggeredFalse();
        for(PriceAlert pricealert:alerts){
            Stock stock=stockRepository.findBySymbol(pricealert.getSymbol()).orElse(null);
            if(stock==null)continue;

            boolean hit=(pricealert.getAlertType()==AlertType.ABOVE&&stock.getPrice()>=pricealert.getTargetPrice())||(pricealert.getAlertType()==AlertType.BELOW&&stock.getPrice()<=pricealert.getTargetPrice());
            if(hit){
                notificationService.notify(pricealert.getUsername(),"Price Alert Triggered", pricealert.getSymbol()+" crossed ₹"+pricealert.getTargetPrice());
                pricealert.setTriggered(true);
                priceAlertRepository.save(pricealert);
            }
        }
    }
}
