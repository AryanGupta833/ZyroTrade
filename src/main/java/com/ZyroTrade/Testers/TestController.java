package com.ZyroTrade.Testers;


import com.ZyroTrade.Price.ExternalPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class TestController {
    @Autowired
    private ExternalPriceService externalPriceService;

    @GetMapping("/refresh-prices")
    public String refreshPrices() {
        externalPriceService.refreshPrices();
        return "OK";
    }
}
