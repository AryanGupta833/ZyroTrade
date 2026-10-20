package com.ZyroTrade.Notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.ZyroTrade.DTO.NotificationResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping
    public List<NotificationResponse> myNotifications() {
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return notificationRepository
                .findByUsernameOrderByCreatedAtDesc(username).stream().map(NotificationResponse::from).toList();
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Notification n = notificationRepository.findByIdAndUsername(id, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        n.setRead(true);
        notificationRepository.save(n);
    }
}
