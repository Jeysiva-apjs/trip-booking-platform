package com.jeysiva.booking.saga;

import com.jeysiva.booking.trip.TripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Saga step 3: approved -> confirm the trip; declined -> compensate (release seat + room, cancel).
@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final TripService tripService;

    public PaymentResultListener(TripService tripService) {
        this.tripService = tripService;
    }

    @KafkaListener(topics = SagaTopics.PAYMENT_RESULTS, groupId = "booking")
    public void onPaymentResult(PaymentResultEvent event) {
        log.info("Received payment result for trip {} — approved={}", event.tripId(), event.approved());
        tripService.applyPaymentResult(event.tripId(), event.approved(), event.paymentId());
    }
}
