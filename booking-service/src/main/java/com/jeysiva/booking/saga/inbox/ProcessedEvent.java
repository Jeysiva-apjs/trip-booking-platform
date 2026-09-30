package com.jeysiva.booking.saga.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A record that one event has already been handled. The event id IS the primary key, so the database
 * itself refuses a second insert of the same event.
 *
 * <p>Kafka delivers at-least-once, so the same message can arrive twice. The row is written in the SAME
 * transaction as the work it marks, which is what makes it trustworthy: you can never end up having done
 * the work without the marker, or having the marker without the work.
 */
@Entity
@Table(name = "processed_event")
public class ProcessedEvent {

    @Id
    private String eventId;

    @Column(nullable = false, updatable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
