package com.jeysiva.booking.hotel;

import com.jeysiva.booking.hotel.dto.CreateHotelRequest;
import com.jeysiva.booking.hotel.dto.HotelResponse;
import com.jeysiva.booking.hotel.dto.RoomResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@Tag(name = "Hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping
    public ResponseEntity<HotelResponse> create(@Valid @RequestBody CreateHotelRequest request,
                                                UriComponentsBuilder uriBuilder) {
        Hotel hotel = hotelService.createHotel(request);
        URI location = uriBuilder.path("/api/hotels/{id}").buildAndExpand(hotel.getId()).toUri();
        return ResponseEntity.created(location).body(HotelResponse.from(hotel));
    }

    @GetMapping
    public List<HotelResponse> list() {
        return hotelService.listHotels().stream().map(HotelResponse::from).toList();
    }

    @GetMapping("/{id}")
    public HotelResponse get(@PathVariable Long id) {
        return HotelResponse.from(hotelService.getHotel(id));
    }

    @GetMapping("/{id}/rooms")
    public List<RoomResponse> rooms(@PathVariable Long id) {
        return hotelService.getRooms(id).stream().map(RoomResponse::from).toList();
    }
}
