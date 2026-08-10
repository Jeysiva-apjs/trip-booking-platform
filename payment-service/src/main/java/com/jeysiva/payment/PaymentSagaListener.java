package com.jeysiva.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// Saga step 2: consume a payment request, charge, publish the result. This service never talks to
// booking directly, only via events.
@Component
public class PaymentSagaListener {

    private static final String PAYMENT_REQUESTS = "payment-requests";
    private static final String PAYMENT_RESULTS = "payment-results";

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaListener.class);

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentSagaListener(PaymentService paymentService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = PAYMENT_REQUESTS, groupId = "payment")
    public void onPaymentRequested(PaymentRequestedEvent event) {
        log.info("Payment requested for trip {} amount {}", event.tripId(), event.amount());

        // charge() is keyed on tripReference, so a redelivered request returns the existing payment
        // instead of charging twice.
        PaymentService.Outcome outcome = paymentService.charge(event.tripReference(), event.amount());

        PaymentResultEvent result = new PaymentResultEvent(
                event.tripId(), event.tripReference(), outcome.paymentId(), outcome.approved(), outcome.reason());
        kafkaTemplate.send(PAYMENT_RESULTS, event.tripReference(), result);
        log.info("Published payment result for trip {} — approved={}", event.tripId(), outcome.approved());
    }
}
