package com.ZyroTrade.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "ix_outbox_due", columnList = "status,next_attempt_at,id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_outbox_event_uuid", columnNames = "event_uuid"),
        @UniqueConstraint(name = "uq_outbox_type_aggregate", columnNames = {"event_type", "aggregate_id"})
})
@Getter
@Setter
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_uuid", nullable = false, length = 36, updatable = false)
    private String eventUuid;

    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private Long aggregateId;

    @Lob
    @Column(nullable = false, updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    @Column(length = 160)
    private String lastError;

    @PrePersist
    void onCreate() {
        if (eventUuid == null) eventUuid = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (nextAttemptAt == null) nextAttemptAt = createdAt;
        if (status == null) status = OutboxStatus.PENDING;
    }
}
