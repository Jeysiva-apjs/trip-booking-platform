package com.jeysiva.booking.saga.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * An event waiting to be published to Kafka.
 *
 * <p>This row is written inside the SAME database transaction as the booking it belongs to. That is the
 * whole point of the outbox: a database commit and a Kafka send cannot be made atomic with each other, so
 * instead of sending during the booking we write the event next to the trip and let a background job send
 * it afterwards. Either both the trip and its event commit, or neither does — the event can never be lost
 * because the process died between the commit and the send.
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    private String id;

    /** Kafka topic this goes to. */
    @Column(nullable = false)
    private String topic;

    /** Kafka message key — the tripReference, so all events for one trip keep their order. */
    @Column(nullable = false)
    private String msgKey;

    /** The event itself, as JSON. Stored as text so the outbox does not care what event type it holds. */
    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Null until the background publisher has sent it. That null is what "still to send" means. */
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String topic, String msgKey, String payload) {
        this.id = UUID.randomUUID().toString();
        this.topic = topic;
        this.msgKey = msgKey;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getMsgKey() {
        return msgKey;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
