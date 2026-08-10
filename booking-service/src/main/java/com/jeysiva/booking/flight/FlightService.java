package com.jeysiva.booking.flight;

import com.jeysiva.booking.common.LockingStrategy;
import com.jeysiva.booking.common.NotFoundException;
import com.jeysiva.booking.common.ResourceUnavailableException;
import com.jeysiva.booking.flight.dto.CreateFlightRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FlightService {

    private static final Logger log = LoggerFactory.getLogger(FlightService.class);

    private static final char[] SEAT_LETTERS = {'A', 'B', 'C', 'D', 'E', 'F'};

    private final FlightRepository flightRepository;
    private final SeatRepository seatRepository;
    private final LockingStrategy lockingStrategy;

    public FlightService(FlightRepository flightRepository,
                         SeatRepository seatRepository,
                         @Value("${booking.locking-strategy:OPTIMISTIC}") LockingStrategy lockingStrategy) {
        this.flightRepository = flightRepository;
        this.seatRepository = seatRepository;
        this.lockingStrategy = lockingStrategy;
        log.info("Seat reservation locking strategy: {}", lockingStrategy);
    }

    @Transactional
    public Flight createFlight(CreateFlightRequest request) {
        Flight flight = new Flight(
                request.flightNumber(),
                request.origin(),
                request.destination(),
                request.departureTime());
        for (int i = 0; i < request.seatCount(); i++) {
            flight.addSeat(new Seat(generateSeatNumber(i), SeatStatus.AVAILABLE));
        }
        return flightRepository.save(flight);
    }

    @Transactional(readOnly = true)
    public Flight getFlight(Long id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Flight " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Flight> listFlights() {
        return flightRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Seat> getSeats(Long flightId) {
        return getFlight(flightId).getSeats();
    }

    // Check-then-act is a race on its own — two threads can both see AVAILABLE. The configured strategy
    // decides how that is closed:
    //   OPTIMISTIC  — plain read; the @Version column on Seat rejects the loser at write time.
    //   PESSIMISTIC — SELECT ... FOR UPDATE; the loser blocks at read time until the winner commits.
    // Either way this joins bookTrip's transaction.
    public Seat reserveSeat(Long seatId) {
        Seat seat = lockingStrategy == LockingStrategy.PESSIMISTIC
                ? findSeatForUpdate(seatId)
                : findSeat(seatId);
        if (!seat.isAvailable()) {
            throw new ResourceUnavailableException("Seat " + seatId + " is not available");
        }
        seat.setStatus(SeatStatus.RESERVED);
        return seatRepository.save(seat);
    }

    // Pessimistic path: loads the seat holding a FOR UPDATE row lock. Only valid inside a transaction.
    private Seat findSeatForUpdate(Long seatId) {
        return seatRepository.findByIdForUpdate(seatId)
                .orElseThrow(() -> new NotFoundException("Seat " + seatId + " not found"));
    }

    public Seat releaseSeat(Long seatId) {
        Seat seat = findSeat(seatId);
        seat.setStatus(SeatStatus.AVAILABLE);
        return seatRepository.save(seat);
    }

    public Seat findSeat(Long seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException("Seat " + seatId + " not found"));
    }

    // 0-based index -> airline style: 1A..1F, 2A..2F, ...
    private String generateSeatNumber(int index) {
        int row = index / SEAT_LETTERS.length + 1;
        char letter = SEAT_LETTERS[index % SEAT_LETTERS.length];
        return row + String.valueOf(letter);
    }
}
