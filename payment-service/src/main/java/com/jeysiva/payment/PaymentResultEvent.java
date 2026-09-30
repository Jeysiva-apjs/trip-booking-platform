package com.jeysiva.payment;

// The outcome payment publishes back to booking (payment-results topic).
public record PaymentResultEvent(
        String eventId,
        Long tripId,
        String tripReference,
        String paymentId,
        boolean approved,
        String reason
) {
}
