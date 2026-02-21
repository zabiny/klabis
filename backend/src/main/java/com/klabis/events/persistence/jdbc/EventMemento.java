package com.klabis.events.persistence.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.Event;
import com.klabis.events.EventId;
import com.klabis.events.EventStatus;
import com.klabis.events.WebsiteUrl;
import com.klabis.users.UserId;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Memento pattern implementation for Event aggregate persistence.
 * <p>
 * This class acts as a bridge between the pure domain {@link Event} entity
 * and Spring Data JDBC persistence. It contains:
 * <ul>
 *   <li>All JDBC annotations for persistence</li>
 *   <li>Flat primitive fields matching the database schema</li>
 *   <li>Conversion methods to/from Event</li>
 *   <li>Domain event delegation for Spring Modulith</li>
 * </ul>
 * <p>
 * The Event entity remains a pure domain object without Spring annotations,
 * while this memento handles all infrastructure concerns.
 */
@Table("events")
class EventMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("name")
    private String name;

    @Column("event_date")
    private LocalDate eventDate;

    @Column("location")
    private String location;

    @Column("organizer")
    private String organizer;

    @Column("website_url")
    private String websiteUrl;

    @Column("event_coordinator_id")
    private UUID eventCoordinatorId;

    @Column("status")
    private String status;

    // Registrations are part of the aggregate
    // Using Set instead of List to avoid needing a position/key column
    @MappedCollection(idColumn = "event_id")
    private Set<EventRegistrationMemento> registrations = new HashSet<>();

    // Audit fields
    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @LastModifiedDate
    @Column("modified_at")
    private Instant lastModifiedAt;

    @LastModifiedBy
    @Column("modified_by")
    private String lastModifiedBy;

    @Version
    @Column("version")
    private Long version;

    // Transient reference to Event for domain event delegation
    @Transient
    private Event event;

    // Transient flag for Persistable<UUID>
    @Transient
    private boolean isNew = true;

    /**
     * Default constructor required by Spring Data JDBC.
     */
    protected EventMemento() {
    }

    /**
     * Creates an EventMemento from an Event entity (for save operations).
     *
     * @param event the Event entity to convert
     * @return a new EventMemento with all fields copied from the Event
     */
    static EventMemento from(Event event) {
        Assert.notNull(event, "Event must not be null");

        EventMemento memento = new EventMemento();

        copyBasicEventInfo(event, memento);
        copyRegistrations(event, memento);
        copyAuditMetadata(event, memento);

        // Store reference for domain events
        memento.event = event;

        // Set isNew flag based on whether event has audit metadata
        // New events (no audit metadata yet) -> INSERT (isNew = true)
        // Existing events (have audit metadata) -> UPDATE (isNew = false)
        memento.isNew = (event.getCreatedAt() == null);

        return memento;
    }

    /**
     * Copies basic event information from Event to memento.
     */
    private static void copyBasicEventInfo(Event event, EventMemento memento) {
        memento.id = event.getId() != null ? event.getId().value() : null;
        memento.name = event.getName();
        memento.eventDate = event.getEventDate();
        memento.location = event.getLocation();
        memento.organizer = event.getOrganizer();
        memento.websiteUrl = event.getWebsiteUrl() != null ? event.getWebsiteUrl().value() : null;
        memento.eventCoordinatorId = event.getEventCoordinatorId() != null ? event.getEventCoordinatorId()
                .uuid() : null;
        memento.status = event.getStatus().name();
    }

    /**
     * Copies event registrations from Event to memento.
     */
    private static void copyRegistrations(Event event, EventMemento memento) {
        memento.registrations = event.getRegistrations().stream()
                .map(EventRegistrationMemento::from)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Copies audit metadata from Event to memento.
     */
    private static void copyAuditMetadata(Event event, EventMemento memento) {
        memento.createdAt = event.getCreatedAt();
        memento.createdBy = event.getCreatedBy();
        memento.lastModifiedAt = event.getLastModifiedAt();
        memento.lastModifiedBy = event.getLastModifiedBy();
    }

    /**
     * Converts this EventMemento to an Event entity (for load operations).
     *
     * @return a reconstituted Event entity
     */
    Event toEvent() {
        EventId eventId = new EventId(this.id);
        WebsiteUrl websiteUrlObj = this.websiteUrl != null ? new WebsiteUrl(this.websiteUrl) : null;
        UserId coordinatorId = this.eventCoordinatorId != null ? new UserId(this.eventCoordinatorId) : null;
        EventStatus eventStatus = EventStatus.valueOf(this.status);

        return Event.reconstruct(
                eventId,
                this.name,
                this.eventDate,
                this.location,
                this.organizer,
                websiteUrlObj,
                coordinatorId,
                eventStatus,
                registrations.stream().map(EventRegistrationMemento::toEventRegistration).toList(),
                new AuditMetadata(
                        this.createdAt,
                        this.createdBy,
                        this.lastModifiedAt,
                        this.lastModifiedBy,
                        this.version)
        );
    }

    /**
     * Delegates domain events from the underlying Event entity.
     * This enables Spring Modulith's transactional outbox pattern for event publication.
     *
     * @return list of domain events to publish
     */
    @org.springframework.data.domain.DomainEvents
    List<Object> domainEvents() {
        return event != null ? event.getDomainEvents() : List.of();
    }

    /**
     * Clears domain events after publication.
     * Called by Spring Data after events have been published.
     */
    @org.springframework.data.domain.AfterDomainEventPublication
    void clearDomainEvents() {
        if (event != null) {
            event.clearDomainEvents();
        }
    }

    // Persistable<UUID> methods
    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

}
