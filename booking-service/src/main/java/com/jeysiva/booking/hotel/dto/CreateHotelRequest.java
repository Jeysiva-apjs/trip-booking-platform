package com.jeysiva.booking.hotel.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Request body for creating a hotel with {@code roomCount} auto-generated available rooms. */
public record CreateHotelRequest(
        @NotBlank String name,
        @NotBlank String location,
        @Min(1) int roomCount
) {
}
