package com.ZyroTrade.Notification;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String title;

    private String message;
    @Column(name = "is_read")
    private boolean isRead;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate(){
        createdAt=LocalDateTime.now();
        isRead=false;

    }
}
