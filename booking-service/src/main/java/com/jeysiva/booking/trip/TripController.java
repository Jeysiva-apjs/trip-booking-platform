package com.jeysiva.booking.trip;

import com.jeysiva.booking.trip.dto.CreateTripRequest;
import com.jeysiva.booking.trip.dto.TripResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
@Tag(name = "Trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    // The owner is whatever userId the request body carries — there is no authentication, so nothing
    // verifies that the caller is that user.
    //
    // 202 Accepted, not 201: the trip is PENDING and settles asynchronously once payment responds.
    @PostMapping
    public ResponseEntity<TripResponse> book(@Valid @RequestBody CreateTripRequest request,
                                             UriComponentsBuilder uriBuilder) {
        Trip trip = tripService.bookTrip(request);
        URI location = uriBuilder.path("/api/trips/{id}").buildAndExpand(trip.getId()).toUri();
        return ResponseEntity.accepted().location(location).body(TripResponse.from(trip));
    }

    /** {@code ?userId=} filters to one user's trips; without it, every trip is returned. */
    @GetMapping
    public List<TripResponse> list(@RequestParam(required = false) Long userId) {
        return tripService.listTrips(userId).stream().map(TripResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TripResponse get(@PathVariable Long id) {
        return TripResponse.from(tripService.getTrip(id));
    }

    @PostMapping("/{id}/cancel")
    public TripResponse cancel(@PathVariable Long id) {
        return TripResponse.from(tripService.cancelTrip(id));
    }
}
