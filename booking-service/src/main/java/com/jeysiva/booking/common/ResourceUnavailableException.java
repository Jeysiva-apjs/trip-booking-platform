package com.jeysiva.booking.common;

/** A seat or room is not AVAILABLE. Maps to HTTP 409. */
public class ResourceUnavailableException extends RuntimeException {
    public ResourceUnavailableException(String message) {
        super(message);
    }
}
