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
public record EventFinishedEvent(
        UUID occurrenceId,
        EventId eventId,
        Instant occurredAt
) {

    public EventFinishedEvent {
        Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public static EventFinishedEvent fromAggregate(Event event) {
        return new EventFinishedEvent(UUID.randomUUID(), event.getId(), Instant.now());
    }
}
