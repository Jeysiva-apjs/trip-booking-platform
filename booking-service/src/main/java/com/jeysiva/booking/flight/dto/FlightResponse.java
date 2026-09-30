package com.jeysiva.booking.flight.dto;

import com.jeysiva.booking.flight.Flight;

import java.time.LocalDateTime;
import java.util.List;

/** API view of a flight, including its seats and a convenience count of what's still available. */
public record FlightResponse(
        Long id,
        String flightNumber,
        String origin,
        String destination,
        LocalDateTime departureTime,
        long availableSeats,
        List<SeatResponse> seats
) {
    public static FlightResponse from(Flight flight) {
        List<SeatResponse> seatViews = flight.getSeats().stream()
                .map(SeatResponse::from)
                .toList();
        long available = flight.getSeats().stream().filter(s -> s.isAvailable()).count();
        return new FlightResponse(
                flight.getId(),
                flight.getFlightNumber(),
                flight.getOrigin(),
                flight.getDestination(),
                flight.getDepartureTime(),
                available,
                seatViews
        );
    }
}
