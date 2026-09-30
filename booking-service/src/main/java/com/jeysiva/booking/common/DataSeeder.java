package com.jeysiva.booking.common;

import com.jeysiva.booking.flight.Flight;
import com.jeysiva.booking.flight.FlightService;
import com.jeysiva.booking.flight.dto.CreateFlightRequest;
import com.jeysiva.booking.hotel.Hotel;
import com.jeysiva.booking.hotel.HotelService;
import com.jeysiva.booking.hotel.dto.CreateHotelRequest;
import com.jeysiva.booking.user.Role;
import com.jeysiva.booking.user.User;
import com.jeysiva.booking.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Seeds one sample flight and hotel on an empty database so booking can be tried immediately. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final FlightService flightService;
    private final HotelService hotelService;
    private final UserRepository userRepository;

    public DataSeeder(FlightService flightService,
                      HotelService hotelService,
                      UserRepository userRepository) {
        this.flightService = flightService;
        this.hotelService = hotelService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        Long demoUserId = seedUsers();

        if (!flightService.listFlights().isEmpty()) {
            return;
        }
        Flight flight = flightService.createFlight(new CreateFlightRequest(
                "AI-101", "BLR", "SIN", LocalDateTime.now().plusDays(7), 6));
        Hotel hotel = hotelService.createHotel(new CreateHotelRequest(
                "Marina Bay Suites", "Singapore", 4));
        log.info("Seeded flight {} and hotel {}. Try: POST /api/trips "
                        + "{{\"seatId\": {}, \"roomId\": {}, \"userId\": {}}}",
                flight.getFlightNumber(), hotel.getName(),
                flight.getSeats().get(0).getId(), hotel.getRooms().get(0).getId(), demoUserId);
    }

    /** Two accounts so trips have plausible owners to be booked against. Returns the demo user's id. */
    private Long seedUsers() {
        seedUser("admin", Role.ADMIN);
        return seedUser("demo", Role.USER);
    }

    private Long seedUser(String username, Role role) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User user = userRepository.save(new User(username, role));
                    log.info("Seeded {} user '{}' (id {}) — pass that id as userId when booking",
                            role, username, user.getId());
                    return user;
                })
                .getId();
    }
}
