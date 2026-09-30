package com.jeysiva.booking.common;

/** Thrown when a requested resource (flight, hotel, seat, room, trip) does not exist. Maps to HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
