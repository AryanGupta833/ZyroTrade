package com.ZyroTrade.Notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping
    public List<Notification> myNotifications() {
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return notificationRepository
                .findByUsernameOrderByCreatedAtDesc(username);
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow();

        n.setRead(true);
        notificationRepository.save(n);
    }
}
