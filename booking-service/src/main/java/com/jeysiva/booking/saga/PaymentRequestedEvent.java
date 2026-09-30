package com.jeysiva.booking.saga;

import java.math.BigDecimal;

// Published by booking: "trip N needs paying". Each service keeps its own copy of the event shape rather
// than sharing a jar, so the two stay independently deployable.
//
// eventId uniquely identifies this event. Kafka can deliver the same message twice, so consumers use it
// to tell "a new event" from "the one I already handled".
public record PaymentRequestedEvent(
        String eventId,
        Long tripId,
        String tripReference,
        BigDecimal amount
) {
}
