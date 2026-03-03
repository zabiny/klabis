package com.klabis.events;

import com.klabis.events.domain.Event;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when an Event is finished.
 *
 * <p>This event can be used to trigger actions such as:
 * - Closing registration system
 * - Generating results and statistics
 * - Sending thank you notifications to participants
 * - Archiving event data
 * - Creating audit log entries
 *
 * <p>Domain events are immutable and represent facts that have already occurred.
 */
@DomainEvent
public class EventFinishedEvent {

    private final UUID occurrenceId;
    private final EventId eventId;
    private final Instant occurredAt;

    private EventFinishedEvent(UUID occurrenceId, EventId eventId, Instant occurredAt) {
        this.occurrenceId = Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        this.eventId = Objects.requireNonNull(eventId, "Event ID is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public static EventFinishedEvent fromAggregate(Event event) {
        return new EventFinishedEvent(UUID.randomUUID(), event.getId(), Instant.now());
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
        EventFinishedEvent that = (EventFinishedEvent) o;
        return Objects.equals(occurrenceId, that.occurrenceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(occurrenceId);
    }

    @Override
    public String toString() {
        return "EventFinishedEvent{" +
               "occurrenceId=" + occurrenceId +
               ", eventId=" + eventId +
               ", occurredAt=" + occurredAt +
               '}';
    }
}
