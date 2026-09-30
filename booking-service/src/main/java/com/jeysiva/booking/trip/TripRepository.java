package com.jeysiva.booking.trip;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    // Underscore = property path seat.id (Trip has no seatId attribute).
    long countBySeat_Id(Long seatId);

    // Ownership filter for GET /api/trips. No underscore needed — userId is a scalar attribute on Trip.
    List<Trip> findByUserId(Long userId);
}
