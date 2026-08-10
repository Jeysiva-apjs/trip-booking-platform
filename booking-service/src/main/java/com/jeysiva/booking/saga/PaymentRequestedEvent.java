package com.jeysiva.booking.saga;

import java.math.BigDecimal;

// Published by booking: "trip N needs paying". Each service keeps its own copy of the event shape rather
// than sharing a jar, so the two stay independently deployable.
public record PaymentRequestedEvent(
        Long tripId,
        String tripReference,
        BigDecimal amount
) {
}
