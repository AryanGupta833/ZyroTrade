package com.ZyroTrade.PriceAlert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceAlertRepository extends JpaRepository<PriceAlert,Long> {
    List<PriceAlert> findByUsernameAndTriggeredFalse(String username);
    List<PriceAlert> findByUsername(String username);
    List<PriceAlert> findByTriggeredFalse();


    Optional<PriceAlert> findByIdAndUsername(Long id,String username);
}
