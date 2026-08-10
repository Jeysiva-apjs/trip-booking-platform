package com.jeysiva.booking.hotel;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for hotels. */
public interface HotelRepository extends JpaRepository<Hotel, Long> {
}
