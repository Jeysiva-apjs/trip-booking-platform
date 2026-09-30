package com.jeysiva.booking.saga;

import com.jeysiva.booking.flight.Flight;
import com.jeysiva.booking.flight.FlightService;
import com.jeysiva.booking.flight.dto.CreateFlightRequest;
import com.jeysiva.booking.hotel.Hotel;
import com.jeysiva.booking.hotel.HotelService;
import com.jeysiva.booking.hotel.dto.CreateHotelRequest;
import com.jeysiva.booking.saga.outbox.OutboxEvent;
import com.jeysiva.booking.saga.outbox.OutboxRepository;
import com.jeysiva.booking.trip.Trip;
import com.jeysiva.booking.trip.TripRepository;
import com.jeysiva.booking.trip.TripService;
import com.jeysiva.booking.trip.TripStatus;
import com.jeysiva.booking.trip.dto.CreateTripRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The two guarantees the outbox + dedupe work is there for. The background publisher is switched off so
 * the outbox row stays put and can be inspected; no Kafka broker is involved anywhere in this test.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.listener.auto-startup=false",
        "eureka.client.enabled=false",
        "booking.outbox.publisher-enabled=false"
})
class OutboxAndDedupeTest {

    private static final Long TEST_USER_ID = 1L;

    @Autowired
    private FlightService flightService;
    @Autowired
    private HotelService hotelService;
    @Autowired
    private TripService tripService;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void bookingWritesThePaymentEventToTheOutboxInsteadOfSendingIt() {
        Trip trip = bookOneTrip();

        List<OutboxEvent> unsent = outboxRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc().stream()
                .filter(row -> row.getMsgKey().equals(trip.getTripReference()))
                .toList();

        Assertions.assertEquals(1, unsent.size(), "booking must leave exactly one unsent outbox row");
        OutboxEvent row = unsent.get(0);
        Assertions.assertEquals(SagaTopics.PAYMENT_REQUESTS, row.getTopic());
        Assertions.assertNull(row.getPublishedAt(), "the row is unsent until the publisher drains it");
        Assertions.assertTrue(row.getPayload().contains(trip.getTripReference()),
                "the payload is the payment request for this trip");
    }

    @Test
    void thesamePaymentResultAppliedTwiceConfirmsTheTripOnce() {
        Trip trip = bookOneTrip();
        String eventId = UUID.randomUUID().toString();

        tripService.applyPaymentResult(eventId, trip.getId(), true, "pay-1");
        // Exactly what a Kafka redelivery looks like: same event id, same payload, second time.
        tripService.applyPaymentResult(eventId, trip.getId(), true, "pay-2");

        Trip settled = tripRepository.findById(trip.getId()).orElseThrow();
        Assertions.assertEquals(TripStatus.CONFIRMED, settled.getStatus());
        Assertions.assertEquals("pay-1", settled.getPaymentId(),
                "the duplicate must be ignored, not overwrite the first result");
    }

    private Trip bookOneTrip() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Flight flight = flightService.createFlight(new CreateFlightRequest(
                "OBX-" + suffix, "BLR", "SIN", LocalDateTime.now().plusDays(30), 1));
        Hotel hotel = hotelService.createHotel(new CreateHotelRequest(
                "ObxHotel-" + suffix, "Testville", 1));

        return tripService.bookTrip(new CreateTripRequest(
                flight.getSeats().get(0).getId(), hotel.getRooms().get(0).getId(), TEST_USER_ID));
    }
}
