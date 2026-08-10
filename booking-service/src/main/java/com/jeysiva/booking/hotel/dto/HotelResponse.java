package com.jeysiva.booking.hotel.dto;

import com.jeysiva.booking.hotel.Hotel;

import java.util.List;

/** API view of a hotel, including its rooms and how many remain available. */
public record HotelResponse(
        Long id,
        String name,
        String location,
        long availableRooms,
        List<RoomResponse> rooms
) {
    public static HotelResponse from(Hotel hotel) {
        List<RoomResponse> roomViews = hotel.getRooms().stream()
                .map(RoomResponse::from)
                .toList();
        long available = hotel.getRooms().stream().filter(r -> r.isAvailable()).count();
        return new HotelResponse(hotel.getId(), hotel.getName(), hotel.getLocation(), available, roomViews);
    }
}
