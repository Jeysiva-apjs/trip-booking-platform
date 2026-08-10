package com.jeysiva.booking.flight;

import com.jeysiva.booking.flight.dto.CreateFlightRequest;
import com.jeysiva.booking.flight.dto.FlightResponse;
import com.jeysiva.booking.flight.dto.SeatResponse;
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
@RequestMapping("/api/flights")
@Tag(name = "Flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @PostMapping
    public ResponseEntity<FlightResponse> create(@Valid @RequestBody CreateFlightRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        Flight flight = flightService.createFlight(request);
        URI location = uriBuilder.path("/api/flights/{id}").buildAndExpand(flight.getId()).toUri();
        return ResponseEntity.created(location).body(FlightResponse.from(flight));
    }

    @GetMapping
    public List<FlightResponse> list() {
        return flightService.listFlights().stream().map(FlightResponse::from).toList();
    }

    @GetMapping("/{id}")
    public FlightResponse get(@PathVariable Long id) {
        return FlightResponse.from(flightService.getFlight(id));
    }

    @GetMapping("/{id}/seats")
    public List<SeatResponse> seats(@PathVariable Long id) {
        return flightService.getSeats(id).stream().map(SeatResponse::from).toList();
    }
}
