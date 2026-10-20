package com.ZyroTrade.Orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface  OrderRepository extends JpaRepository<Order,Long> {
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    List<Order> findByUserUsernameOrderByCreatedAtDesc(String username);
    Optional<Order> findByIdAndUserUsername(Long id, String username);
    Page<Order> findByUserUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
    long countByUserUsername(String username);
    @Query(value = "SELECT o.id FROM orders o JOIN users u ON u.id = o.user_id " +
            "LEFT JOIN journal_entries j ON j.business_reference = CONCAT('ORDER:', o.id) " +
            "WHERE u.username = :username AND j.id IS NULL ORDER BY o.id", nativeQuery = true)
    List<Long> findMissingLedgerOrderIds(@Param("username") String username);

    List<Order> findByUserUsernameAndType(String username, OrderType orderType);
    @Query("""
SELECT COALESCE(SUM(o.realizedPnl), 0)
FROM Order o
WHERE o.user.username = :username
AND o.type = 'SELL'
""")
    java.math.BigDecimal sumRealizedPnl(@Param("username") String username);

}
