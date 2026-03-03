package com.klabis.events;

import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

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

    private final UUID occurrenceId;
    private final EventId eventId;
    private final Instant occurredAt;

    private EventPublishedEvent(UUID occurrenceId, EventId eventId, Instant occurredAt) {
        this.occurrenceId = Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        this.eventId = Objects.requireNonNull(eventId, "Event ID is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public static EventPublishedEvent fromAggregate(Event event) {
        return new EventPublishedEvent(UUID.randomUUID(), event.getId(), Instant.now());
    }

    public UUID getOccurrenceId() {
        return occurrenceId;
    }

    public EventId getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventPublishedEvent that = (EventPublishedEvent) o;
        return Objects.equals(occurrenceId, that.occurrenceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(occurrenceId);
    }

    @Override
    public String toString() {
        return "EventPublishedEvent{" +
               "occurrenceId=" + occurrenceId +
               ", eventId=" + eventId +
               ", occurredAt=" + occurredAt +
               '}';
    }
}
