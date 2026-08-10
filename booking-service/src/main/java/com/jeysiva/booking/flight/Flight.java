package com.jeysiva.booking.flight;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flights")
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String flightNumber;

    private String origin;
    private String destination;
    private LocalDateTime departureTime;

    // Seat is the owning side (it holds the flight_id column); mappedBy makes this the inverse side.
    // Cascade lets one save() persist the flight and its seats together.
    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();

    protected Flight() {
    }

    public Flight(String flightNumber, String origin, String destination, LocalDateTime departureTime) {
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
    }

    /** Keeps both sides of the bidirectional association in sync. */
    public void addSeat(Seat seat) {
        seats.add(seat);
        seat.setFlight(this);
    }

    public Long getId() {
        return id;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public List<Seat> getSeats() {
        return seats;
    }
}
