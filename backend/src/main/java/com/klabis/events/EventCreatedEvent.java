package com.klabis.events;

import com.klabis.common.users.UserId;
import com.klabis.events.domain.Event;
import com.klabis.events.WebsiteUrl;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain event published when a new Event is created.
 *
 * <p>This event can be used to trigger post-creation actions such as:
 * - Sending notifications to coordinators
 * - Creating audit log entries
 * - Notifying other bounded contexts
 * - Provisioning related resources (e.g., registration system)
 *
 * <p>Domain events are immutable and represent facts that have already occurred.
 *
 * <p><b>Event Publishing:</b> Published synchronously within the transaction.
 * Uses Spring Modulith's transactional outbox pattern for reliable,
 * exactly-once event delivery with guaranteed consistency.
 */
@DomainEvent
public record EventCreatedEvent(
        UUID occurrenceId,
        EventId eventId,
        String name,
        LocalDate eventDate,
        String location,
        String organizer,
        WebsiteUrl websiteUrl,
        UserId eventCoordinatorId,
        Instant occurredAt
) {

    public EventCreatedEvent {
        Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        Objects.requireNonNull(eventId, "Event ID is required");
        Objects.requireNonNull(name, "Event name is required");
        Objects.requireNonNull(eventDate, "Event date is required");
        Objects.requireNonNull(location, "Event location is required");
        Objects.requireNonNull(organizer, "Event organizer is required");
        Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    public static EventCreatedEvent fromAggregate(Event event) {
        return new EventCreatedEvent(
                UUID.randomUUID(),
                event.getId(),
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getOrganizer(),
                event.getWebsiteUrl(),
                event.getEventCoordinatorId(),
                event.getCreatedAt() != null ? event.getCreatedAt() : Instant.now()
        );
    }

    public Optional<WebsiteUrl> getWebsiteUrl() {
        return Optional.ofNullable(websiteUrl());
    }

    public Optional<UserId> getEventCoordinatorId() {
        return Optional.ofNullable(eventCoordinatorId());
    }
}
