package com.klabis.events;

import com.klabis.events.domain.Event;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when an Event is updated in DRAFT or ACTIVE status (not in FINISHED or CANCELLED).
 *
 * <p>Carries only the event id and the update origin; listeners load the current event data through
 * the events module's ports.
 */
@RecordBuilder
@DomainEvent
public record EventUpdatedEvent(
        UUID occurrenceId,
        EventId eventId,
        Instant occurredAt,
        UpdateOrigin origin
) {

    public EventUpdatedEvent {
        Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
        Objects.requireNonNull(origin, "Update origin is required");
    }

    public static EventUpdatedEvent fromAggregate(Event event, UpdateOrigin origin) {
        return new EventUpdatedEvent(UUID.randomUUID(), event.getId(), Instant.now(), origin);
    }
}
