package com.jeysiva.booking.trip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeysiva.booking.common.NotFoundException;
import com.jeysiva.booking.flight.FlightService;
import com.jeysiva.booking.flight.Seat;
import com.jeysiva.booking.hotel.HotelService;
import com.jeysiva.booking.hotel.Room;
import com.jeysiva.booking.saga.PaymentRequestedEvent;
import com.jeysiva.booking.saga.SagaTopics;
import com.jeysiva.booking.saga.inbox.ProcessedEvent;
import com.jeysiva.booking.saga.inbox.ProcessedEventRepository;
import com.jeysiva.booking.saga.outbox.OutboxEvent;
import com.jeysiva.booking.saga.outbox.OutboxRepository;
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
    private final OutboxRepository outboxRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;
    private final BigDecimal tripPrice;

    public TripService(FlightService flightService,
                       HotelService hotelService,
                       TripRepository tripRepository,
                       OutboxRepository outboxRepository,
                       ProcessedEventRepository processedEventRepository,
                       ObjectMapper objectMapper,
                       @Value("${booking.trip-price:250.00}") BigDecimal tripPrice) {
        this.flightService = flightService;
        this.hotelService = hotelService;
        this.tripRepository = tripRepository;
        this.outboxRepository = outboxRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
        this.tripPrice = tripPrice;
    }

    // Saga step 1: reserve seat + room, save the trip as PENDING, and write the payment request to the
    // outbox — all in ONE transaction. Nothing is sent to Kafka here.
    //
    // Sending to Kafka inside this method would not be covered by the transaction: a crash between the
    // database commit and the send would leave the seat and room reserved and the trip PENDING forever,
    // with payment never told to charge. Writing the event as a row instead makes it part of the same
    // commit, and OutboxPublisher sends it a moment later.
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

        PaymentRequestedEvent event = new PaymentRequestedEvent(
                UUID.randomUUID().toString(), trip.getId(), tripReference, tripPrice);
        outboxRepository.save(new OutboxEvent(
                SagaTopics.PAYMENT_REQUESTS, tripReference, toJson(event)));

        log.info("Trip {} PENDING — payment request queued in the outbox", trip.getId());
        return trip;
    }

    // Saga step 3. Two guards, both needed:
    //   1. eventId — has this exact message already been handled? Kafka delivers at-least-once, so the
    //      same result can arrive twice. The marker is saved in THIS transaction, so the work and the
    //      "done" record commit together or not at all.
    //   2. PENDING — is the trip still open? Belt and braces, and it also covers a duplicate result that
    //      arrives with a different eventId.
    @Transactional
    public void applyPaymentResult(String eventId, Long tripId, boolean approved, String paymentId) {
        if (eventId != null && processedEventRepository.existsById(eventId)) {
            log.info("Ignoring payment result {} for trip {} — already processed", eventId, tripId);
            return;
        }

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

        if (eventId != null) {
            processedEventRepository.save(new ProcessedEvent(eventId));
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

    private String toJson(PaymentRequestedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            // Cannot happen for a plain record; if it ever did, failing the booking is the right answer
            // rather than committing a trip whose payment event is unreadable.
            throw new IllegalStateException("Could not serialize " + event, e);
        }
    }
}
