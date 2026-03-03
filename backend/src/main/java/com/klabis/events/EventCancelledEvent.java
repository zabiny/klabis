package com.klabis.events;

import com.klabis.events.domain.Event;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when an Event is cancelled.
 *
 * <p>This event can be used to trigger actions such as:
 * - Closing registration system
 * - Sending cancellation notifications to participants
 * - Refunding registration fees
 * - Removing event from public listings
 * - Creating audit log entries
 *
 * <p>Domain events are immutable and represent facts that have already occurred.
 */
@DomainEvent
public record EventCancelledEvent(
        UUID occurrenceId,
        EventId eventId,
        Instant occurredAt
) {

    public EventCancelledEvent {
        Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public static EventCancelledEvent fromAggregate(Event event) {
        return new EventCancelledEvent(UUID.randomUUID(), event.getId(), Instant.now());
    }
}
