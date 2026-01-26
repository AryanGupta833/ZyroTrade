package com.ZyroTrade.Portfolios;


import com.ZyroTrade.DTO.PnlResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pnl")
public class PnlController {

    @Autowired
    private PnlService pnlService;

    @GetMapping("/unrealized")
    public List<PnlResponse> unrealized(){
        String username= SecurityContextHolder.getContext().getAuthentication().getName();
        return pnlService.unrealizedPnl(username);
    }
}
