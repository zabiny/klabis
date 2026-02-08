package com.klabis.events;

import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event published when an Event is published (transitioned to ACTIVE status).
 *
 * <p>This event can be used to trigger actions such as:
 * - Opening registration system
 * - Sending notifications to potential participants
 * - Publishing event to public listings
 * - Creating audit log entries
 *
 * <p>Domain events are immutable and represent facts that have already occurred.
 */
@DomainEvent
public class EventPublishedEvent {

    private final EventId eventId;
    private final Instant occurredAt;

    /**
     * Creates a new EventPublishedEvent with the current timestamp.
     *
     * @param eventId the unique identifier of the published event
     */
    public EventPublishedEvent(EventId eventId) {
        this(
                eventId,
                Instant.now()  // Use current time
        );
    }

    /**
     * Creates a new EventPublishedEvent with explicit timestamp.
     * Useful for testing and event reconstruction.
     *
     * @param eventId    the unique identifier of the published event
     * @param occurredAt the timestamp when this event occurred
     */
    public EventPublishedEvent(
            EventId eventId,
            Instant occurredAt) {

        this.eventId = Objects.requireNonNull(eventId, "Event ID is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    // Getters

    /**
     * Get the event identifier.
     *
     * @return event identifier
     */
    public EventId getEventId() {
        return eventId;
    }

    /**
     * Get the timestamp when this event occurred.
     *
     * @return event occurrence timestamp
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventPublishedEvent that = (EventPublishedEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "EventPublishedEvent{" +
               "eventId=" + eventId +
               ", occurredAt=" + occurredAt +
               '}';
    }
}
