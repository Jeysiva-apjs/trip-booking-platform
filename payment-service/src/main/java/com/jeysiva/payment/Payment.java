package com.jeysiva.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

// Audit trail: every charge is a durable row, so "was this trip paid for?" can always be answered and a
// refund can be issued against the id.
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private String id;   // UUID, not auto-increment: sequential ids are guessable and leak volume

    @Column(nullable = false)
    private String tripReference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public Payment(String id, String tripReference, BigDecimal amount, Instant createdAt) {
        this.id = id;
        this.tripReference = tripReference;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getTripReference() {
        return tripReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
