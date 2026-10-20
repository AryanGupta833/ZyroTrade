package com.ZyroTrade.outbox;

import com.ZyroTrade.Notification.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxTradeNotificationConsumer {
    private final OutboxInboxRepository inbox;
    private final NotificationService notifications;

    public OutboxTradeNotificationConsumer(OutboxInboxRepository inbox, NotificationService notifications) {
        this.inbox = inbox;
        this.notifications = notifications;
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTradeCompleted(TradeCompletedEvent event) {
        if (inbox.existsByEventUuid(event.eventId())) return;
        try {
            inbox.saveAndFlush(new OutboxInboxReceipt(event.eventId()));
        } catch (DataIntegrityViolationException concurrentDuplicate) {
            return;
        }
        notifications.notify(event.username(), "Order Executed",
                event.type() + " " + event.quantity() + " shares of " + event.symbol());
    }
}
