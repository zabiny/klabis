package com.klabis.events;

import com.klabis.events.domain.Event;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when an Event is updated.
 *
 * <p>This event includes all updatable Event fields to enable event-driven synchronization
 * with dependent systems (e.g., Calendar module) without querying the Events repository.
 *
 * <p>This event is published when:
 * - Event is updated in DRAFT status
 * - Event is updated in ACTIVE status
 *
 * <p>This event is NOT published when:
 * - Event is in FINISHED status
 * - Event is in CANCELLED status
 *
 * <p>Domain events are immutable and represent facts that have already occurred.
 */
@RecordBuilder
@DomainEvent
public record EventUpdatedEvent(
        UUID occurrenceId,
        EventId eventId,
        String name,
        LocalDate eventDate,
        String location,
        String organizer,
        WebsiteUrl websiteUrl,
        List<String> categories,
        Instant occurredAt
) {

    public EventUpdatedEvent {
        Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(name, "Event name is required");
        Objects.requireNonNull(eventDate, "Event date is required");
        Objects.requireNonNull(organizer, "Event organizer is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
        categories = categories != null ? List.copyOf(categories) : List.of();
    }

    public static EventUpdatedEvent fromAggregate(Event event) {
        return new EventUpdatedEvent(
                UUID.randomUUID(),
                event.getId(),
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getOrganizer(),
                event.getWebsiteUrl(),
                event.getCategories(),
                Instant.now()
        );
    }
}
