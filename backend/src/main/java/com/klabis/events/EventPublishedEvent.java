package com.klabis.events;

import com.klabis.events.domain.Event;
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
public record EventPublishedEvent(
        UUID occurrenceId,
        EventId eventId,
        Instant occurredAt
) {

    public EventPublishedEvent {
        Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public static EventPublishedEvent fromAggregate(Event event) {
        return new EventPublishedEvent(UUID.randomUUID(), event.getId(), Instant.now());
    }
}
