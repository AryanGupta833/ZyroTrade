package com.ZyroTrade.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxInboxRepository extends JpaRepository<OutboxInboxReceipt, Long> {
    boolean existsByEventUuid(String eventUuid);
}
