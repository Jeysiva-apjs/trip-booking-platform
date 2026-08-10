package com.jeysiva.booking.flight.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateFlightRequest(
        @NotBlank String flightNumber,
        @NotBlank String origin,
        @NotBlank String destination,
        @NotNull @Future LocalDateTime departureTime,
        @Min(1) int seatCount
) {
}
