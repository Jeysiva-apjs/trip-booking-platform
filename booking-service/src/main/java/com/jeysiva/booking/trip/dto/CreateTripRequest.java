package com.jeysiva.booking.trip.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for booking a trip: which seat and which room to reserve together, and who the trip
 * belongs to. The owner is client-supplied because the platform has no authentication to derive it from.
 */
public record CreateTripRequest(
        @NotNull Long seatId,
        @NotNull Long roomId,
        @NotNull Long userId
) {
}
