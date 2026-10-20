package com.ZyroTrade.outbox;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record OutboxEventSummary(Long id, String eventUuid, String eventType, Long aggregateId,
                                 OutboxStatus status, int attempts, OffsetDateTime nextAttemptAt, OffsetDateTime createdAt,
                                 OffsetDateTime publishedAt, String lastError) {
    public static OutboxEventSummary from(OutboxEvent event) {
        return new OutboxEventSummary(event.getId(), event.getEventUuid(), event.getEventType(), event.getAggregateId(),
                event.getStatus(), event.getAttempts(), offset(event.getNextAttemptAt()), offset(event.getCreatedAt()),
                event.getPublishedAt() == null ? null : offset(event.getPublishedAt()), event.getLastError());
    }

    private static OffsetDateTime offset(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
