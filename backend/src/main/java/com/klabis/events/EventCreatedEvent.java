package com.klabis.events;

import com.klabis.common.users.UserId;
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
public class EventCreatedEvent {

    private final UUID occurrenceId;
    private final EventId eventId;
    private final String name;
    private final LocalDate eventDate;
    private final String location;
    private final String organizer;
    private final WebsiteUrl websiteUrl;
    private final UserId eventCoordinatorId;
    private final Instant occurredAt;

    private EventCreatedEvent(
            UUID occurrenceId,
            EventId eventId,
            String name,
            LocalDate eventDate,
            String location,
            String organizer,
            WebsiteUrl websiteUrl,
            UserId eventCoordinatorId,
            Instant occurredAt) {

        this.occurrenceId = Objects.requireNonNull(occurrenceId, "Occurrence ID is required");
        this.eventId = Objects.requireNonNull(eventId, "Event ID is required");
        this.name = Objects.requireNonNull(name, "Event name is required");
        this.eventDate = Objects.requireNonNull(eventDate, "Event date is required");
        this.location = Objects.requireNonNull(location, "Event location is required");
        this.organizer = Objects.requireNonNull(organizer, "Event organizer is required");
        this.websiteUrl = websiteUrl;
        this.eventCoordinatorId = eventCoordinatorId;
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
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

    public UUID getOccurrenceId() {
        return occurrenceId;
    }

    public EventId getEventId() {
        return eventId;
    }

    public String getName() {
        return name;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public String getLocation() {
        return location;
    }

    public String getOrganizer() {
        return organizer;
    }

    public Optional<WebsiteUrl> getWebsiteUrl() {
        return Optional.ofNullable(websiteUrl);
    }

    public Optional<UserId> getEventCoordinatorId() {
        return Optional.ofNullable(eventCoordinatorId);
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventCreatedEvent that = (EventCreatedEvent) o;
        return Objects.equals(occurrenceId, that.occurrenceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(occurrenceId);
    }

    @Override
    public String toString() {
        return "EventCreatedEvent{" +
               "occurrenceId=" + occurrenceId +
               ", eventId=" + eventId +
               ", name='" + name + '\'' +
               ", eventDate=" + eventDate +
               ", location='" + location + '\'' +
               ", organizer='" + organizer + '\'' +
               ", occurredAt=" + occurredAt +
               '}';
    }
}
