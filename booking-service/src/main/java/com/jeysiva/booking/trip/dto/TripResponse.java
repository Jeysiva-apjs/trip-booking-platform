package com.jeysiva.booking.trip.dto;

import com.jeysiva.booking.trip.Trip;
import com.jeysiva.booking.trip.TripStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** API view of a trip, including what paid for it. */
public record TripResponse(
        Long id,
        Long seatId,
        Long roomId,
        TripStatus status,
        Instant createdAt,
        String tripReference,
        String paymentId,
        BigDecimal amountPaid
) {
    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getSeatId(),
                trip.getRoomId(),
                trip.getStatus(),
                trip.getCreatedAt(),
                trip.getTripReference(),
                trip.getPaymentId(),
                trip.getAmountPaid());
    }
}
