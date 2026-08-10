package com.jeysiva.booking.saga;

// The payment outcome booking consumes back. approved=false drives the saga's compensation.
public record PaymentResultEvent(
        Long tripId,
        String tripReference,
        String paymentId,
        boolean approved,
        String reason
) {
}
