package com.jeysiva.booking.saga;

/** Kafka topic names shared by the booking↔payment saga. */
public final class SagaTopics {

    // booking -> payment: "please charge this trip"
    public static final String PAYMENT_REQUESTS = "payment-requests";

    // payment -> booking: "charge succeeded / failed"
    public static final String PAYMENT_RESULTS = "payment-results";

    private SagaTopics() {
    }
}
