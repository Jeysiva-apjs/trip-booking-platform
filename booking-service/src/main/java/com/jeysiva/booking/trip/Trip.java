package com.jeysiva.booking.trip;

import com.jeysiva.booking.flight.Seat;
import com.jeysiva.booking.hotel.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** A trip = one reserved flight seat + one reserved hotel room, booked together. */
@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A plain id rather than a @ManyToOne to User: this only records who the trip is for, and keeping it
    // a scalar avoids dragging the user table into every trip query. Nullable because trips booked before
    // ownership existed have no owner. Client-supplied and unvalidated — see TripService#bookTrip.
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Plain id, not a FK — the payment row lives in the payment service's own database.
    private String paymentId;

    // Correlation id — the same value the payment service stores, so a charge can be traced back to this
    // booking across the two separate databases.
    private String tripReference;

    @Column(precision = 12, scale = 2)
    private BigDecimal amountPaid;

    protected Trip() {
    }

    public Trip(Long userId, Seat seat, Room room, TripStatus status, Instant createdAt,
                String tripReference, String paymentId, BigDecimal amountPaid) {
        this.userId = userId;
        this.seat = seat;
        this.room = room;
        this.status = status;
        this.createdAt = createdAt;
        this.tripReference = tripReference;
        this.paymentId = paymentId;
        this.amountPaid = amountPaid;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Seat getSeat() {
        return seat;
    }

    public Room getRoom() {
        return room;
    }

    public Long getSeatId() {
        return seat != null ? seat.getId() : null;
    }

    public Long getRoomId() {
        return room != null ? room.getId() : null;
    }

    public TripStatus getStatus() {
        return status;
    }

    /** PENDING -> CONFIRMED, recording which payment paid for it. */
    public void confirm(String paymentId) {
        this.status = TripStatus.CONFIRMED;
        this.paymentId = paymentId;
    }

    public void cancel() {
        this.status = TripStatus.CANCELLED;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getTripReference() {
        return tripReference;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }
}
