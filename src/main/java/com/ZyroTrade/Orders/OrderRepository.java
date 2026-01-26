package com.ZyroTrade.Orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface  OrderRepository extends JpaRepository<Order,Long> {
    List<Order> findByUserUsernameOrderByCreatedAtDesc(String username);

    List<Order> findByUserUsernameAndType(String username, OrderType orderType);
    @Query("""
SELECT COALESCE(SUM(o.realizedPnl), 0)
FROM Order o
WHERE o.user.username = :username
AND o.type = 'SELL'
""")
    double sumRealizedPnl(@Param("username") String username);

}
