package com.klabis.events;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Event aggregate root.
 * <p>
 * Represents an orienteering event with details about name, date, location, and coordinator.
 * This is the aggregate root for the Events bounded context.
 * <p>
 * Business invariants:
 * - Name, event date, location, and organizer are required
 * - Website URL and event coordinator are optional
 * - Events start in DRAFT status
 * - Status transitions follow defined lifecycle rules
 * - Updates only allowed in DRAFT and ACTIVE status
 *
 * <p>Persistence:
 * - This aggregate root is a pure domain object without Spring Data JDBC annotations
 * - ID is stored as UUID in database, exposed as EventId value object in domain
 * - Value objects are maintained as fields for domain logic
 * - Domain events are published via Spring Modulith's transactional outbox pattern
 */
@AggregateRoot
public class Event extends KlabisAggregateRoot<Event, EventId> {

    @Identity
    private final EventId id;

    // Event details
    private String name;
    private LocalDate eventDate;
    private String location;
    private String organizer;
    private WebsiteUrl websiteUrl;
    @Association
    private UserId eventCoordinatorId;
    private EventStatus status;

    // Event registrations
    private final List<EventRegistration> registrations = new ArrayList<>();

    // ========== Nested Command Records ==========

    public record CreateCommand(
            @NotBlank(message = "Event name is required")
            @Size(max = 100, message = "Event name must not exceed 100 characters")
            String name,

            @NotNull(message = "Event date is required")
            LocalDate eventDate,

            @NotBlank(message = "Event location is required")
            @Size(max = 100, message = "Event location must not exceed 100 characters")
            String location,

            @NotBlank(message = "Event organizer is required")
            @Size(max = 10, message = "Event organizer must not exceed 10 characters")
            String organizer,

            @URL(message = "Website URL must be valid")
            String websiteUrl,

            UUID eventCoordinatorId
    ) {
    }

    public record UpdateCommand(
            @NotBlank(message = "Event name is required")
            @Size(max = 100, message = "Event name must not exceed 100 characters")
            String name,

            @NotNull(message = "Event date is required")
            LocalDate eventDate,

            @NotBlank(message = "Event location is required")
            @Size(max = 100, message = "Event location must not exceed 100 characters")
            String location,

            @NotBlank(message = "Event organizer is required")
            @Size(max = 10, message = "Event organizer must not exceed 10 characters")
            String organizer,

            @URL(message = "Website URL must be valid")
            String websiteUrl,

            UUID eventCoordinatorId
    ) {
    }

    public record RegisterCommand(
            @NotBlank(message = "SI card number is required")
            @Pattern(regexp = "\\d{6,7}", message = "SI card number must be 6-7 digits")
            String siCardNumber
    ) {
    }

    /**
     * Private constructor for creating new Event instances.
     * <p>
     * This constructor is used by the static factory methods (create, reconstruct)
     * to ensure business invariants are validated during construction.
     */
    private Event(
            EventId id,
            String name,
            LocalDate eventDate,
            String location,
            String organizer,
            WebsiteUrl websiteUrl,
            UserId eventCoordinatorId,
            EventStatus status,
            AuditMetadata auditMetadata) {

        this.id = id;
        this.name = name;
        this.eventDate = eventDate;
        this.location = location;
        this.organizer = organizer;
        this.websiteUrl = websiteUrl;
        this.eventCoordinatorId = eventCoordinatorId;
        this.status = status;
        updateAuditMetadata(auditMetadata);
    }

    /**
     * Factory method for reconstructing Event from persistence layer.
     * This bypasses validation since the data was already validated when originally stored.
     * <p>
     * This method is public only for infrastructure/persistence layer usage.
     * Use {@link #create(String, LocalDate, String, String, WebsiteUrl, UserId)} for creating new events.
     *
     * @param id                 event's unique identifier
     * @param name               event name
     * @param eventDate          event date
     * @param location           event location
     * @param organizer          event organizer
     * @param websiteUrl         event website URL (may be null)
     * @param eventCoordinatorId event coordinator ID (may be null)
     * @param status             event status
     * @return reconstructed Event instance
     */
    public static Event reconstruct(
            EventId id,
            String name,
            LocalDate eventDate,
            String location,
            String organizer,
            WebsiteUrl websiteUrl,
            UserId eventCoordinatorId,
            EventStatus status,
            List<EventRegistration> registrations,
            AuditMetadata auditMetadata) {

        Event event = new Event(
                id,
                name,
                eventDate,
                location,
                organizer,
                websiteUrl,
                eventCoordinatorId,
                status,
                auditMetadata
        );
        event.registrations.addAll(registrations);
        // No domain events for reconstructed entities
        return event;
    }

    /**
     * Static factory method to create a new Event.
     * <p>
     * Creates a new event in DRAFT status with a generated ID.
     * The event must be published using {@link #publish()} to make it ACTIVE.
     *
     * @param name               event name (required)
     * @param eventDate          event date (required)
     * @param location           event location (required)
     * @param organizer          event organizer (required)
     * @param websiteUrl         event website URL (may be null)
     * @param eventCoordinatorId event coordinator ID (may be null)
     * @return new Event instance in DRAFT status
     * @throws IllegalArgumentException if business rules are violated
     */
    public static Event create(
            String name,
            LocalDate eventDate,
            String location,
            String organizer,
            WebsiteUrl websiteUrl,
            UserId eventCoordinatorId) {

        // Validate required fields
        validateName(name);
        validateEventDate(eventDate);
        validateLocation(location);
        validateOrganizer(organizer);

        Event event = new Event(
                EventId.generate(),
                name,
                eventDate,
                location,
                organizer,
                websiteUrl,
                eventCoordinatorId,
                EventStatus.DRAFT,
                null
        );

        // Register domain event
        event.registerEvent(EventCreatedEvent.fromAggregate(event));

        return event;
    }

    // ========== Validation Methods ==========

    private static void validateName(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("Event name is required");
        }
    }

    private static void validateEventDate(LocalDate eventDate) {
        if (eventDate == null) {
            throw new IllegalArgumentException("eventDate is required");
        }
    }

    private static void validateLocation(String location) {
        if (location == null || location.trim().isBlank()) {
            throw new IllegalArgumentException("Event location is required");
        }
    }

    private static void validateOrganizer(String organizer) {
        if (organizer == null || organizer.trim().isBlank()) {
            throw new IllegalArgumentException("Event organizer is required");
        }
    }

    // ========== Getters ==========

    @Override
    public EventId getId() {
        return id;
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

    public WebsiteUrl getWebsiteUrl() {
        return websiteUrl;
    }

    public UserId getEventCoordinatorId() {
        return eventCoordinatorId;
    }

    public EventStatus getStatus() {
        return status;
    }

    // ========== Domain Methods ==========

    /**
     * Publishes the event, transitioning it from DRAFT to ACTIVE status.
     * <p>
     * Business rule: Only DRAFT events can be published.
     *
     * @throws IllegalStateException if transition is not allowed
     */
    public void publish() {
        status.validateTransition(EventStatus.ACTIVE);
        this.status = EventStatus.ACTIVE;

        // Register domain event
        registerEvent(EventPublishedEvent.fromAggregate(this));
    }

    /**
     * Cancels the event, transitioning it to CANCELLED status.
     * <p>
     * Business rule: Only DRAFT and ACTIVE events can be cancelled.
     *
     * @throws IllegalStateException if transition is not allowed
     */
    public void cancel() {
        status.validateTransition(EventStatus.CANCELLED);
        this.status = EventStatus.CANCELLED;

        // Register domain event
        registerEvent(EventCancelledEvent.fromAggregate(this));
    }

    /**
     * Finishes the event, transitioning it from ACTIVE to FINISHED status.
     * <p>
     * Business rule: Only ACTIVE events can be finished.
     *
     * @throws IllegalStateException if transition is not allowed
     */
    public void finish() {
        status.validateTransition(EventStatus.FINISHED);
        this.status = EventStatus.FINISHED;

        // Register domain event
        registerEvent(EventFinishedEvent.fromAggregate(this));
    }

    /**
     * Updates event details.
     * <p>
     * Business rule: Events can only be updated in DRAFT or ACTIVE status.
     * Updates are forbidden for FINISHED and CANCELLED events.
     *
     * @param name               new event name (required)
     * @param eventDate          new event date (required)
     * @param location           new event location (required)
     * @param organizer          new event organizer (required)
     * @param websiteUrl         new website URL (may be null)
     * @param eventCoordinatorId new coordinator ID (may be null)
     * @throws IllegalStateException    if event is in FINISHED or CANCELLED status
     * @throws IllegalArgumentException if validation fails
     */
    public void update(
            String name,
            LocalDate eventDate,
            String location,
            String organizer,
            WebsiteUrl websiteUrl,
            UserId eventCoordinatorId) {

        // Check status allows updates
        if (status == EventStatus.FINISHED) {
            throw new BusinessRuleViolationException("Cannot update event in FINISHED status") {
            };
        }
        if (status == EventStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Cannot update event in CANCELLED status") {
            };
        }

        // Validate required fields
        validateName(name);
        validateEventDate(eventDate);
        validateLocation(location);
        validateOrganizer(organizer);

        // Modify fields in-place
        this.name = name;
        this.eventDate = eventDate;
        this.location = location;
        this.organizer = organizer;
        this.websiteUrl = websiteUrl;
        this.eventCoordinatorId = eventCoordinatorId;

        // Register domain event
        registerEvent(EventUpdatedEvent.fromAggregate(this));
    }

    // ========== Registration Methods ==========

    /**
     * Register a member for this event.
     * <p>
     * Business rules:
     * - Registration is only allowed for ACTIVE events
     * - Member cannot be registered twice for the same event (domain invariant)
     *
     * @param memberId     member's user ID (required)
     * @param siCardNumber SI card number (required)
     * @throws BusinessRuleViolationException if event is not ACTIVE
     * @throws IllegalStateException          if member is already registered (domain invariant violation)
     */
    public void registerMember(MemberId memberId, SiCardNumber siCardNumber) {
        // Check event status
        if (status != EventStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Registration is only allowed for ACTIVE events") {
                @Override
                public synchronized Throwable fillInStackTrace() {
                    return super.fillInStackTrace();
                }
            };
        }

        // Check for duplicate registration
        if (findRegistration(memberId).isPresent()) {
            throw new IllegalStateException("Duplicate registration not allowed for this event");
        }

        // Create and add registration
        EventRegistration registration = EventRegistration.create(memberId, siCardNumber);
        registrations.add(registration);

        // Register domain event
        registerEvent(MemberRegisteredForEventEvent.fromAggregate(this, memberId));
    }

    /**
     * Unregister a member from this event.
     * <p>
     * Business rule: Unregistration is only allowed before the event date.
     *
     * @param memberId    member's user ID (required)
     * @param currentDate current date for validation
     * @throws IllegalStateException    if current date is on or after event date
     * @throws IllegalArgumentException if member is not registered
     */
    public void unregisterMember(MemberId memberId, LocalDate currentDate) {
        // Check if unregistration is allowed (before event date)
        if (!currentDate.isBefore(eventDate)) {
            throw new BusinessRuleViolationException("Cannot unregister on or after event date") {
            };
        }

        // Find and remove registration
        EventRegistration registration = findRegistration(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member is not registered for this event"));

        registrations.remove(registration);

        // Register domain event
        registerEvent(MemberUnregisteredFromEventEvent.fromAggregate(this, memberId));
    }

    /**
     * Find a registration by member ID.
     *
     * @param memberId member's user ID
     * @return Optional containing the registration if found, empty otherwise
     */
    public Optional<EventRegistration> findRegistration(MemberId memberId) {
        return registrations.stream()
                .filter(r -> r.memberId().equals(memberId))
                .findFirst();
    }

    /**
     * Get all registrations for this event.
     *
     * @return unmodifiable list of registrations
     */
    public List<EventRegistration> getRegistrations() {
        return Collections.unmodifiableList(registrations);
    }

    @Override
    public String toString() {
        return "Event{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", eventDate=" + eventDate +
               ", location='" + location + '\'' +
               ", organizer='" + organizer + '\'' +
               ", status=" + status +
               '}';
    }
}
