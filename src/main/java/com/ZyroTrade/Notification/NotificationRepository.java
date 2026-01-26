package com.ZyroTrade.Notification;

import org.aspectj.weaver.ast.Not;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
    List<Notification> findByUsernameOrderByCreatedAtDesc(String username);
}
