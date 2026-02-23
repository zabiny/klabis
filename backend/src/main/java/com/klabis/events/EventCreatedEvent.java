package com.klabis.events;

import com.klabis.common.users.UserId;
import org.jmolecules.event.annotation.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

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

    private final EventId eventId;
    private final String name;
    private final LocalDate eventDate;
    private final String location;
    private final String organizer;
    private final WebsiteUrl websiteUrl;
    private final UserId eventCoordinatorId;
    private final Instant occurredAt;

    /**
     * Creates a new EventCreatedEvent with the current timestamp.
     *
     * @param eventId            the unique identifier of the created event
     * @param name               the event name
     * @param eventDate          the event date
     * @param location           the event location
     * @param organizer          the event organizer
     * @param websiteUrl         the event website URL (may be null)
     * @param eventCoordinatorId the event coordinator ID (may be null)
     */
    public EventCreatedEvent(
            EventId eventId,
            String name,
            LocalDate eventDate,
            String location,
            String organizer,
            WebsiteUrl websiteUrl,
            UserId eventCoordinatorId) {
        this(
                eventId,
                name,
                eventDate,
                location,
                organizer,
                websiteUrl,
                eventCoordinatorId,
                Instant.now()  // Use current time
        );
    }

    /**
     * Creates a new EventCreatedEvent with explicit timestamp.
     * Useful for testing and event reconstruction.
     *
     * @param eventId            the unique identifier of the created event
     * @param name               the event name
     * @param eventDate          the event date
     * @param location           the event location
     * @param organizer          the event organizer
     * @param websiteUrl         the event website URL (may be null)
     * @param eventCoordinatorId the event coordinator ID (may be null)
     * @param occurredAt         the timestamp when this event occurred
     */
    public EventCreatedEvent(
            EventId eventId,
            String name,
            LocalDate eventDate,
            String location,
            String organizer,
            WebsiteUrl websiteUrl,
            UserId eventCoordinatorId,
            Instant occurredAt) {

        this.eventId = Objects.requireNonNull(eventId, "Event ID is required");
        this.name = Objects.requireNonNull(name, "Event name is required");
        this.eventDate = Objects.requireNonNull(eventDate, "Event date is required");
        this.location = Objects.requireNonNull(location, "Event location is required");
        this.organizer = Objects.requireNonNull(organizer, "Event organizer is required");
        this.websiteUrl = websiteUrl;
        this.eventCoordinatorId = eventCoordinatorId;
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    /**
     * Factory method to create event from Event aggregate.
     *
     * @param event the event that was created
     * @return new EventCreatedEvent
     */
    public static EventCreatedEvent fromEvent(Event event) {
        return new EventCreatedEvent(
                event.getId(),
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getOrganizer(),
                event.getWebsiteUrl(),
                event.getEventCoordinatorId()
        );
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

    /**
     * Get event website URL.
     *
     * @return Optional containing website URL, or empty if not provided
     */
    public Optional<WebsiteUrl> getWebsiteUrl() {
        return Optional.ofNullable(websiteUrl);
    }

    /**
     * Get event coordinator ID.
     *
     * @return Optional containing coordinator ID, or empty if not provided
     */
    public Optional<UserId> getEventCoordinatorId() {
        return Optional.ofNullable(eventCoordinatorId);
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
        EventCreatedEvent that = (EventCreatedEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    /**
     * Returns a string representation for logging.
     *
     * @return safe string representation for logs
     */
    @Override
    public String toString() {
        return "EventCreatedEvent{" +
               "eventId=" + eventId +
               ", name='" + name + '\'' +
               ", eventDate=" + eventDate +
               ", location='" + location + '\'' +
               ", organizer='" + organizer + '\'' +
               ", occurredAt=" + occurredAt +
               '}';
    }
}
