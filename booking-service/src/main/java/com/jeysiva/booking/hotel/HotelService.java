package com.jeysiva.booking.hotel;

import com.jeysiva.booking.common.NotFoundException;
import com.jeysiva.booking.common.ResourceUnavailableException;
import com.jeysiva.booking.hotel.dto.CreateHotelRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HotelService {

    private static final int ROOMS_PER_FLOOR = 10;

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    public HotelService(HotelRepository hotelRepository, RoomRepository roomRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public Hotel createHotel(CreateHotelRequest request) {
        Hotel hotel = new Hotel(request.name(), request.location());
        for (int i = 0; i < request.roomCount(); i++) {
            hotel.addRoom(new Room(generateRoomNumber(i), RoomStatus.AVAILABLE));
        }
        return hotelRepository.save(hotel);
    }

    @Transactional(readOnly = true)
    public Hotel getHotel(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hotel " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Hotel> listHotels() {
        return hotelRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Room> getRooms(Long hotelId) {
        return getHotel(hotelId).getRooms();
    }

    // Runs inside the caller's transaction, like FlightService.reserveSeat.
    public Room reserveRoom(Long roomId) {
        Room room = findRoom(roomId);
        if (!room.isAvailable()) {
            throw new ResourceUnavailableException("Room " + roomId + " is not available");
        }
        room.setStatus(RoomStatus.RESERVED);
        return roomRepository.save(room);
    }

    public Room releaseRoom(Long roomId) {
        Room room = findRoom(roomId);
        room.setStatus(RoomStatus.AVAILABLE);
        return roomRepository.save(room);
    }

    public Room findRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room " + roomId + " not found"));
    }

    // 0-based index -> hotel style: 101..110, 201..210, ...
    private String generateRoomNumber(int index) {
        int floor = index / ROOMS_PER_FLOOR + 1;
        int number = index % ROOMS_PER_FLOOR + 1;
        return String.valueOf(floor * 100 + number);
    }
}
