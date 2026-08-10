package com.jeysiva.payment;

import java.math.BigDecimal;

// Payment's own copy of the event booking publishes (consumed from the payment-requests topic).
public record PaymentRequestedEvent(
        Long tripId,
        String tripReference,
        BigDecimal amount
) {
}
