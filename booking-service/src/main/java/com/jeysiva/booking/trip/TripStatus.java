package com.jeysiva.booking.trip;

// Saga state: PENDING while payment happens asynchronously, then CONFIRMED or CANCELLED once it answers.
public enum TripStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
