package com.jeysiva.booking.flight.dto;

import com.jeysiva.booking.flight.Seat;
import com.jeysiva.booking.flight.SeatStatus;

public record SeatResponse(Long id, Long flightId, String seatNumber, SeatStatus status) {

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getFlightId(), seat.getSeatNumber(), seat.getStatus());
    }
}
