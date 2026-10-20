package com.ZyroTrade.DTO;

import com.ZyroTrade.Notification.Notification;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record NotificationResponse(Long id, String title, String message, boolean read, OffsetDateTime createdAt) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getTitle(), notification.getMessage(),
                notification.isRead(), notification.getCreatedAt() == null ? null
                : notification.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }
}
