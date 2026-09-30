package com.jeysiva.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// Saga step 2: consume a payment request, charge, publish the result. This service never talks to
// booking directly, only via events. Failures are handled by KafkaErrorHandlerConfig (retry, then DLT).
@Component
public class PaymentSagaListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaListener.class);

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentSagaListener(PaymentService paymentService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = SagaTopics.PAYMENT_REQUESTS, groupId = "payment")
    public void onPaymentRequested(PaymentRequestedEvent event) {
        log.info("Payment requested for trip {} amount {}", event.tripId(), event.amount());

        // charge() is keyed on tripReference, so a redelivered request returns the existing payment
        // instead of charging twice. Note this deliberately still publishes a result: if the first
        // result was lost, the redelivery is what settles the trip.
        PaymentService.Outcome outcome = paymentService.charge(event.tripReference(), event.amount());

        // Derived from the request id rather than random, so a redelivered request produces the SAME
        // result id and booking's dedupe recognises it as a duplicate. Messages published before eventId
        // existed have none — give those a random id so they cannot all collide on one value.
        String resultEventId = event.eventId() != null
                ? "result-" + event.eventId()
                : "result-" + java.util.UUID.randomUUID();

        PaymentResultEvent result = new PaymentResultEvent(
                resultEventId, event.tripId(), event.tripReference(),
                outcome.paymentId(), outcome.approved(), outcome.reason());
        kafkaTemplate.send(SagaTopics.PAYMENT_RESULTS, event.tripReference(), result);
        log.info("Published payment result for trip {} — approved={}", event.tripId(), outcome.approved());
    }
}
