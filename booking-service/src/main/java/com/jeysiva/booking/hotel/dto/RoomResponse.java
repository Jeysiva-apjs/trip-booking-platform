package com.jeysiva.booking.hotel.dto;

import com.jeysiva.booking.hotel.Room;
import com.jeysiva.booking.hotel.RoomStatus;

/** API view of a room. */
public record RoomResponse(
        Long id,
        Long hotelId,
        String roomNumber,
        RoomStatus status
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getHotelId(), room.getRoomNumber(), room.getStatus());
    }
}
