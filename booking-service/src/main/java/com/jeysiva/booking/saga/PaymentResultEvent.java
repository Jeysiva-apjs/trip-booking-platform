package com.jeysiva.booking.saga;

// The payment outcome booking consumes back. approved=false drives the saga's compensation.
// eventId is what the result listener dedupes on.
public record PaymentResultEvent(
        String eventId,
        Long tripId,
        String tripReference,
        String paymentId,
        boolean approved,
        String reason
) {
}
