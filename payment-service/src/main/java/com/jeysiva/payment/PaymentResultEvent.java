package com.jeysiva.payment;

// The outcome payment publishes back to booking (payment-results topic).
public record PaymentResultEvent(
        Long tripId,
        String tripReference,
        String paymentId,
        boolean approved,
        String reason
) {
}
