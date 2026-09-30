package com.jeysiva.booking.saga.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeysiva.booking.saga.PaymentEventPublisher;
import com.jeysiva.booking.saga.PaymentRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drains the outbox: every second, take the events that have not been sent yet, push them to Kafka and
 * stamp them as published.
 *
 * <p>Delivery here is at-least-once. If this process dies after the Kafka send but before the row is
 * stamped, the same event is sent again on the next run. That is deliberate and safe — losing an event
 * would strand a trip in PENDING forever, whereas a duplicate is caught downstream by the consumer's
 * dedupe check.
 */
@Component
@ConditionalOnProperty(name = "booking.outbox.publisher-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxRepository outboxRepository,
                           PaymentEventPublisher paymentEventPublisher,
                           ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPending() {
        for (OutboxEvent row : outboxRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                // Booking only ever outboxes PaymentRequestedEvent. If a second event type is ever added,
                // give OutboxEvent a `type` column and switch on it here.
                PaymentRequestedEvent event = objectMapper.readValue(row.getPayload(), PaymentRequestedEvent.class);
                paymentEventPublisher.send(row.getTopic(), row.getMsgKey(), event);
                row.markPublished();
                log.debug("Outbox row {} published to {}", row.getId(), row.getTopic());
            } catch (Exception e) {
                // Leave publishedAt null and try again next tick — a broker that is down must not lose the event.
                log.warn("Outbox row {} could not be published, will retry: {}", row.getId(), e.getMessage());
            }
        }
    }
}
