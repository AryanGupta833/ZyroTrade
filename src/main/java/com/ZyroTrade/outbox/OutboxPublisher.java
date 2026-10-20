package com.ZyroTrade.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {
    private final OutboxService outboxService;

    public OutboxPublisher(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-delay-ms:5000}")
    public void publishDueEvents() {
        outboxService.dispatchPending();
    }
}
