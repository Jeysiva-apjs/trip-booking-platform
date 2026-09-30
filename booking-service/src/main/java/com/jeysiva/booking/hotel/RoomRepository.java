package com.jeysiva.booking.hotel;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for rooms — looked up and updated directly when a trip reserves a room. */
public interface RoomRepository extends JpaRepository<Room, Long> {
}
