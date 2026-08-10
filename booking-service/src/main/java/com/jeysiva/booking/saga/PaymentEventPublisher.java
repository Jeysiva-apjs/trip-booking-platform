package com.jeysiva.booking.saga;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// Booking publishes an event and moves on rather than calling payment, which keeps payment out of the
// booking transaction.
@Component
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void requestPayment(PaymentRequestedEvent event) {
        // Keyed on tripReference so all events for one trip land on the same partition, preserving order.
        kafkaTemplate.send(SagaTopics.PAYMENT_REQUESTS, event.tripReference(), event);
    }
}
