package com.ZyroTrade.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_inbox_receipts", uniqueConstraints =
        @UniqueConstraint(name = "uq_outbox_inbox_event_uuid", columnNames = "event_uuid"))
public class OutboxInboxReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_uuid", nullable = false, length = 36, updatable = false)
    private String eventUuid;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    protected OutboxInboxReceipt() {}

    public OutboxInboxReceipt(String eventUuid) {
        this.eventUuid = eventUuid;
    }

    @PrePersist
    void onCreate() {
        processedAt = LocalDateTime.now();
    }
}
