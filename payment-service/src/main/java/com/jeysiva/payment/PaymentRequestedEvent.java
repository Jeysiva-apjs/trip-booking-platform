package com.jeysiva.payment;

import java.math.BigDecimal;

// Payment's own copy of the event booking publishes (consumed from the payment-requests topic).
// eventId identifies the message; it is echoed into the result so booking can dedupe on it.
public record PaymentRequestedEvent(
        String eventId,
        Long tripId,
        String tripReference,
        BigDecimal amount
) {
}
