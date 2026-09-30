package com.jeysiva.payment;

/** Kafka topic names shared by the booking↔payment saga. */
public final class SagaTopics {

    public static final String PAYMENT_REQUESTS = "payment-requests";
    public static final String PAYMENT_RESULTS = "payment-results";

    // Dead letter topics. The ".DLT" suffix is the default DeadLetterPublishingRecoverer appends.
    public static final String PAYMENT_REQUESTS_DLT = PAYMENT_REQUESTS + ".DLT";
    public static final String PAYMENT_RESULTS_DLT = PAYMENT_RESULTS + ".DLT";

    private SagaTopics() {
    }
}
