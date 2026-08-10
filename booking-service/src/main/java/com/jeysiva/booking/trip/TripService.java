package com.jeysiva.booking.trip;

import com.jeysiva.booking.common.NotFoundException;
import com.jeysiva.booking.flight.FlightService;
import com.jeysiva.booking.flight.Seat;
import com.jeysiva.booking.hotel.HotelService;
import com.jeysiva.booking.hotel.Room;
import com.jeysiva.booking.saga.PaymentEventPublisher;
import com.jeysiva.booking.saga.PaymentRequestedEvent;
import com.jeysiva.booking.trip.dto.CreateTripRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);

    private final FlightService flightService;
    private final HotelService hotelService;
    private final TripRepository tripRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final BigDecimal tripPrice;

    public TripService(FlightService flightService,
                       HotelService hotelService,
                       TripRepository tripRepository,
                       PaymentEventPublisher paymentEventPublisher,
                       @Value("${booking.trip-price:250.00}") BigDecimal tripPrice) {
        this.flightService = flightService;
        this.hotelService = hotelService;
        this.tripRepository = tripRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.tripPrice = tripPrice;
    }

    // Saga step 1: reserve seat + room in one local transaction, save the trip as PENDING, then publish
    // a PaymentRequested event. Payment happens asynchronously so it can't hold DB rows open.
    //
    // The owner comes straight off the request. Nothing checks that the caller is that user, or that the
    // user exists at all — there is no authentication in this platform.
    @Transactional
    public Trip bookTrip(CreateTripRequest request) {
        Seat seat = flightService.reserveSeat(request.seatId());
        Room room = hotelService.reserveRoom(request.roomId());

        String tripReference = "seat-" + seat.getId() + "-room-" + room.getId()
                + "-" + UUID.randomUUID().toString().substring(0, 8);
        Trip trip = tripRepository.save(new Trip(
                request.userId(), seat, room, TripStatus.PENDING, Instant.now(), tripReference, null, tripPrice));

        paymentEventPublisher.requestPayment(new PaymentRequestedEvent(
                trip.getId(), tripReference, tripPrice));
        log.info("Trip {} PENDING — payment requested", trip.getId());
        return trip;
    }

    // Saga step 3. The PENDING guard makes this idempotent: a redelivered event finds the trip already
    // CONFIRMED/CANCELLED and does nothing, so it can't double-confirm or double-compensate.
    @Transactional
    public void applyPaymentResult(Long tripId, boolean approved, String paymentId) {
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null || trip.getStatus() != TripStatus.PENDING) {
            log.info("Ignoring payment result for trip {} (already settled or missing)", tripId);
            return;
        }
        if (approved) {
            trip.confirm(paymentId);
            log.info("Trip {} CONFIRMED (payment {})", tripId, paymentId);
        } else {
            // Compensation: undo the reservations the saga already made.
            flightService.releaseSeat(trip.getSeatId());
            hotelService.releaseRoom(trip.getRoomId());
            trip.cancel();
            log.info("Trip {} CANCELLED — payment failed, seat + room released", tripId);
        }
    }

    // Manual cancellation of a confirmed trip. No refund is issued.
    @Transactional
    public Trip cancelTrip(Long tripId) {
        Trip trip = getTrip(tripId);
        if (trip.getStatus() == TripStatus.CANCELLED) {
            return trip;
        }
        flightService.releaseSeat(trip.getSeatId());
        hotelService.releaseRoom(trip.getRoomId());
        trip.cancel();
        return trip;
    }

    /**
     * Any trip by id. There is no ownership check: without authentication there is no caller identity to
     * compare the trip's owner against, so every trip is readable by anyone who can reach the service.
     */
    @Transactional(readOnly = true)
    public Trip getTrip(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Trip " + id + " not found"));
    }

    /** All trips, or just one user's when {@code userId} is given. The filter is a convenience, not a rule. */
    @Transactional(readOnly = true)
    public List<Trip> listTrips(Long userId) {
        return userId == null
                ? tripRepository.findAll()
                : tripRepository.findByUserId(userId);
    }
}
