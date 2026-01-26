package com.ZyroTrade.PriceAlert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class PriceAlertController {
    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @PostMapping
    public PriceAlert createdAlert(@RequestBody PriceAlert pricealert){
        String username= SecurityContextHolder.getContext().getAuthentication().getName();
        pricealert.setUsername(username);
        pricealert.setTriggered(false);
        return priceAlertRepository.save(pricealert);
    }

    @GetMapping
    public List<PriceAlert> myAlerts(){
        String username=SecurityContextHolder.getContext().getAuthentication().getName();
        return priceAlertRepository.findByUsername(username);

        }

    @DeleteMapping("/{id}")
    public void deleteAlert(@PathVariable Long id) {

        String username =
                SecurityContextHolder.getContext()
                        .getAuthentication().getName();

        PriceAlert alert = priceAlertRepository
                .findByIdAndUsername(id, username)
                .orElseThrow(() ->
                        new RuntimeException("Alert not found or not authorized"));

        priceAlertRepository.delete(alert);
    }

}
