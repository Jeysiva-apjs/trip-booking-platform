package com.jeysiva.booking.saga;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * The thin Kafka send. Nothing in the booking flow calls this directly any more — events are written to
 * the outbox table first and OutboxPublisher calls this when it drains them.
 */
@Component
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Keyed on tripReference so all events for one trip land on the same partition, preserving order. */
    public void send(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event);
    }
}
