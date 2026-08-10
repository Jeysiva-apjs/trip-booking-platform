package com.jeysiva.booking.trip;

import com.jeysiva.booking.flight.Flight;
import com.jeysiva.booking.flight.FlightService;
import com.jeysiva.booking.flight.dto.CreateFlightRequest;
import com.jeysiva.booking.hotel.Hotel;
import com.jeysiva.booking.hotel.HotelService;
import com.jeysiva.booking.hotel.Room;
import com.jeysiva.booking.hotel.dto.CreateHotelRequest;
import com.jeysiva.booking.saga.PaymentEventPublisher;
import com.jeysiva.booking.trip.dto.CreateTripRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

// The seat is reserved synchronously inside bookTrip, before the payment event is published, so the
// @Version check must let exactly one of 20 concurrent bookings win. The Kafka publisher is mocked, so
// no broker is needed and the winning trip stays PENDING.
@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.listener.auto-startup=false",
        "eureka.client.enabled=false"
})
class SeatConcurrencyTest {

    private static final int CONCURRENT_BOOKERS = 20;
    private static final Long TEST_USER_ID = 1L;

    @Autowired
    private FlightService flightService;
    @Autowired
    private HotelService hotelService;
    @Autowired
    private TripService tripService;
    @Autowired
    private TripRepository tripRepository;

    @MockitoBean
    private PaymentEventPublisher paymentEventPublisher;   // no real Kafka publish during the test

    @Test
    void onlyOneBookingWinsTheContestedSeat() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Flight flight = flightService.createFlight(new CreateFlightRequest(
                "CONC-" + suffix, "BLR", "SIN", LocalDateTime.now().plusDays(30), 1));
        Hotel hotel = hotelService.createHotel(new CreateHotelRequest(
                "ConcHotel-" + suffix, "Testville", CONCURRENT_BOOKERS));

        Long contestedSeatId = flight.getSeats().get(0).getId();
        List<Long> roomIds = hotel.getRooms().stream().map(Room::getId).toList();

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_BOOKERS);
        CountDownLatch startGun = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < CONCURRENT_BOOKERS; i++) {
            Long roomId = roomIds.get(i);
            futures.add(pool.submit(() -> {
                startGun.await();
                try {
                    tripService.bookTrip(new CreateTripRequest(contestedSeatId, roomId, TEST_USER_ID));
                    succeeded.incrementAndGet();
                } catch (RuntimeException expectedUnderContention) {
                    rejected.incrementAndGet();
                }
                return null;
            }));
        }

        startGun.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        pool.shutdown();

        Assertions.assertEquals(1, succeeded.get(), "exactly one booking must win the contested seat");
        Assertions.assertEquals(CONCURRENT_BOOKERS - 1, rejected.get(), "every other booking must be rejected");
        Assertions.assertEquals(1, tripRepository.countBySeat_Id(contestedSeatId),
                "the seat must never appear on more than one trip");
    }
}
