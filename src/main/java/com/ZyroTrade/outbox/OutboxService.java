package com.ZyroTrade.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ZyroTrade.Orders.Order;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxService {
    private static final int MAX_ATTEMPTS = 8;
    private static final int BATCH_SIZE = 20;
    private final OutboxEventRepository events;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher publisher;
    private final MeterRegistry meters;

    public OutboxService(OutboxEventRepository events, ObjectMapper objectMapper, ApplicationEventPublisher publisher,
                         MeterRegistry meters) {
        this.events = events;
        this.objectMapper = objectMapper;
        this.publisher = publisher;
        this.meters = meters;
        Gauge.builder("outbox.pending", events, repository -> (double) repository.countByStatus(OutboxStatus.PENDING))
                .register(meters);
    }

    @Transactional
    public OutboxEvent enqueueTradeCompleted(Order order) {
        TradeCompletedPayload payload = new TradeCompletedPayload(order.getId(), order.getUser().getUsername(),
                order.getStock().getSymbol(), order.getType().name(), order.getQuantity(), order.getPrice());
        OutboxEvent event = new OutboxEvent();
        event.setEventType("trade.completed.v1");
        event.setAggregateId(order.getId());
        event.setStatus(OutboxStatus.PENDING);
        try {
            event.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException serializationFailure) {
            throw new IllegalStateException("Trade event could not be serialized", serializationFailure);
        }
        return events.saveAndFlush(event);
    }

    @Transactional
    public int dispatchPending() {
        List<Long> dueIds = events.findDueIds(LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE));
        int delivered = 0;
        for (Long id : dueIds) {
            OutboxEvent event = events.findByIdForUpdate(id).orElse(null);
            if (event == null || event.getStatus() != OutboxStatus.PENDING
                    || event.getNextAttemptAt().isAfter(LocalDateTime.now())) continue;
            try {
                if (!"trade.completed.v1".equals(event.getEventType())) {
                    throw new IllegalStateException("Unsupported outbox event type");
                }
                TradeCompletedPayload payload = objectMapper.readValue(event.getPayload(), TradeCompletedPayload.class);
                publisher.publishEvent(new TradeCompletedEvent(event.getEventUuid(), payload.orderId(), payload.username(),
                        payload.symbol(), payload.type(), payload.quantity(), payload.executionPrice()));
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());
                event.setLastError(null);
                meters.counter("outbox.publication", "outcome", "published").increment();
                delivered++;
            } catch (Exception publicationFailure) {
                int attempts = event.getAttempts() + 1;
                event.setAttempts(attempts);
                event.setLastError(publicationFailure.getClass().getSimpleName());
                if (attempts >= MAX_ATTEMPTS) {
                    event.setStatus(OutboxStatus.FAILED);
                    meters.counter("outbox.publication", "outcome", "dead_letter").increment();
                } else {
                    event.setNextAttemptAt(LocalDateTime.now().plusSeconds(Math.min(300, 1L << Math.min(attempts, 8))));
                    meters.counter("outbox.publication", "outcome", "retry").increment();
                }
            }
        }
        return delivered;
    }

    @Transactional(readOnly = true)
    public List<OutboxEventSummary> latest() {
        return events.findTop100ByOrderByCreatedAtDesc().stream().map(OutboxEventSummary::from).toList();
    }

    @Transactional
    public void replay(Long id) {
        OutboxEvent event = events.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Outbox event not found"));
        if (event.getStatus() != OutboxStatus.FAILED) {
            throw new IllegalArgumentException("Only failed outbox events can be replayed");
        }
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setLastError(null);
        event.setNextAttemptAt(LocalDateTime.now());
    }
}
