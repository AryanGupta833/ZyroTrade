package com.ZyroTrade.Portfolios;

import com.ZyroTrade.DTO.PortfolioPnlResponse;

import com.ZyroTrade.user.User;
import com.ZyroTrade.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.ZyroTrade.DTO.PortfolioHoldingResponse;

@RestController
@RequestMapping("/api/portfolio")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Portfolio", description = "Authenticated user's holdings and P&L")
public class PortfolioController {

    private final PortfolioHoldingRepository  portfolioRepository;
    private final PortfolioPnlService portfolioPnlService;
    @Autowired
    private UserRepository userRepository;

    public PortfolioController(
            PortfolioHoldingRepository portfolioRepository,
            PortfolioPnlService portfolioPnlService) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioPnlService = portfolioPnlService;
    }

    @GetMapping
    public List<PortfolioHoldingResponse> myPortfolio(
            @AuthenticationPrincipal UserDetails user) {

        String username= user.getUsername();
        User user1=userRepository.findByUsername(username).orElseThrow();
        Long userId=user1.getId();
        return portfolioRepository.findByUserid(userId).stream().map(PortfolioHoldingResponse::from).toList();
    }

    @GetMapping("/pnl")
    public PortfolioPnlResponse myPortfolioPnl(
            @AuthenticationPrincipal UserDetails user) {


        return portfolioPnlService.calculatePnlByUsername(user.getUsername());
    }
}
