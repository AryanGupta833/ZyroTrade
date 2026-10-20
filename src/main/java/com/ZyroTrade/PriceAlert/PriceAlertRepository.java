package com.ZyroTrade.PriceAlert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface PriceAlertRepository extends JpaRepository<PriceAlert,Long> {
    List<PriceAlert> findByUsernameAndTriggeredFalse(String username);
    List<PriceAlert> findByUsername(String username);
    List<PriceAlert> findByTriggeredFalse();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PriceAlert a where a.triggered = false")
    List<PriceAlert> findUntriggeredForUpdate();


    Optional<PriceAlert> findByIdAndUsername(Long id,String username);
}
