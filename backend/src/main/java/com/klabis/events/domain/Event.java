package com.klabis.events.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.*;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
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
    private Integer orisId;
    private LocalDate eventDate;
    private String location;
    private String organizer;
    private WebsiteUrl websiteUrl;
    @Association
    private MemberId eventCoordinatorId;
    private LocalDate registrationDeadline;
    private EventStatus status;

    // Event registrations
    private final List<EventRegistration> registrations = new ArrayList<>();

    // ========== Nested Command Records ==========

    @RecordBuilder
    public record CreateEvent(
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

            MemberId eventCoordinatorId,
            LocalDate registrationDeadline
    ) {}

    @RecordBuilder
    public record UpdateEvent(
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

            MemberId eventCoordinatorId,

            LocalDate registrationDeadline
    ) {}

    @RecordBuilder
    public record CreateEventFromOris(
            int orisId,
            String name,
            LocalDate eventDate,
            String location,
            String organizer,
            WebsiteUrl websiteUrl,
            LocalDate registrationDeadline
    ) {}

    @RecordBuilder
    public record RegisterCommand(
            @NotBlank(message = "SI card number is required")
            @Pattern(regexp = "\\d{6,7}", message = "SI card number must be 6-7 digits")
            String siCardNumber
    ) {
    }

    @RecordBuilder
    public record ImportCommand(
            @jakarta.validation.constraints.Positive(message = "ORIS event ID must be positive")
            int orisId
    ) {
    }

    @RecordBuilder
    public record UnregisterMember(
            MemberId memberId
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
            MemberId eventCoordinatorId,
            LocalDate registrationDeadline,
            EventStatus status,
            Integer orisId,
            AuditMetadata auditMetadata) {

        this.id = id;
        this.name = name;
        this.eventDate = eventDate;
        this.location = location;
        this.organizer = organizer;
        this.websiteUrl = websiteUrl;
        this.eventCoordinatorId = eventCoordinatorId;
        this.registrationDeadline = registrationDeadline;
        this.status = status;
        this.orisId = orisId;
        updateAuditMetadata(auditMetadata);
    }

    /**
     * Factory method for reconstructing Event from persistence layer.
     * This bypasses validation since the data was already validated when originally stored.
     * <p>
     * This method is public only for infrastructure/persistence layer usage.
     * Use {@link #create(CreateEvent)} for creating new events.
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
            MemberId eventCoordinatorId,
            LocalDate registrationDeadline,
            EventStatus status,
            Integer orisId,
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
                registrationDeadline,
                status,
                orisId,
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
     * @param command event creation command with all required and optional fields
     * @return new Event instance in DRAFT status
     * @throws IllegalArgumentException if business rules are violated
     */
    public static Event create(CreateEvent command) {
        validateName(command.name());
        validateEventDate(command.eventDate());
        validateLocation(command.location());
        validateOrganizer(command.organizer());
        validateRegistrationDeadline(command.registrationDeadline(), command.eventDate());

        Event event = new Event(
                EventId.generate(),
                command.name(),
                command.eventDate(),
                command.location(),
                command.organizer(),
                command.websiteUrl() != null ? WebsiteUrl.of(command.websiteUrl()) : null,
                command.eventCoordinatorId(),
                command.registrationDeadline(),
                EventStatus.DRAFT,
                null,
                null
        );

        event.registerEvent(EventCreatedEvent.fromAggregate(event));

        return event;
    }

    /**
     * Factory method to create an Event imported from ORIS.
     * <p>
     * Creates a new event in DRAFT status with data sourced from the ORIS orienteering system.
     * The orisId is stored internally and is never exposed in API responses.
     *
     * @param command event creation command with all ORIS-sourced fields
     * @return new Event instance in DRAFT status with orisId set
     */
    public static Event createFromOris(CreateEventFromOris command) {
        validateName(command.name());
        validateEventDate(command.eventDate());
        validateLocation(command.location());
        validateOrganizer(command.organizer());
        validateRegistrationDeadline(command.registrationDeadline(), command.eventDate());

        Event event = new Event(
                EventId.generate(),
                command.name(),
                command.eventDate(),
                command.location(),
                command.organizer(),
                command.websiteUrl(),
                null,
                command.registrationDeadline(),
                EventStatus.DRAFT,
                command.orisId(),
                null
        );

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

    private static void validateRegistrationDeadline(LocalDate registrationDeadline, LocalDate eventDate) {
        if (registrationDeadline != null && eventDate != null && registrationDeadline.isAfter(eventDate)) {
            throw new BusinessRuleViolationException("Registration deadline must be on or before event date") {
            };
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

    public MemberId getEventCoordinatorId() {
        return eventCoordinatorId;
    }

    public LocalDate getRegistrationDeadline() {
        return registrationDeadline;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Integer getOrisId() {
        return orisId;
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
     * @param command update command with all required and optional fields
     * @throws IllegalStateException    if event is in FINISHED or CANCELLED status
     * @throws IllegalArgumentException if validation fails
     */
    public void update(UpdateEvent command) {
        if (status == EventStatus.FINISHED) {
            throw new BusinessRuleViolationException("Cannot update event in FINISHED status") {
            };
        }
        if (status == EventStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Cannot update event in CANCELLED status") {
            };
        }

        validateName(command.name());
        validateEventDate(command.eventDate());
        validateLocation(command.location());
        validateOrganizer(command.organizer());
        validateRegistrationDeadline(command.registrationDeadline(), command.eventDate());

        this.name = command.name();
        this.eventDate = command.eventDate();
        this.location = command.location();
        this.organizer = command.organizer();
        this.websiteUrl = command.websiteUrl() != null ? WebsiteUrl.of(command.websiteUrl()) : null;
        this.eventCoordinatorId = command.eventCoordinatorId();
        this.registrationDeadline = command.registrationDeadline();

        registerEvent(EventUpdatedEvent.fromAggregate(this));
    }

    /**
     * Returns true when registrations are open: the event is ACTIVE, the event date is strictly in the future,
     * and the registration deadline (if set) has not passed.
     */
    public boolean areRegistrationsOpen() {
        LocalDate today = LocalDate.now();
        return status == EventStatus.ACTIVE
                && eventDate.isAfter(today)
                && (registrationDeadline == null || registrationDeadline.isAfter(today));
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
        if (status != EventStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Registration is only allowed for ACTIVE events") {
                @Override
                public synchronized Throwable fillInStackTrace() {
                    return super.fillInStackTrace();
                }
            };
        }

        LocalDate today = LocalDate.now();
        if (registrationDeadline != null && !registrationDeadline.isAfter(today)) {
            throw new BusinessRuleViolationException("Registration deadline has passed") {};
        }

        if (findRegistration(memberId).isPresent()) {
            throw new DuplicateRegistrationException(memberId, this.id);
        }

        // Create and add registration
        EventRegistration registration = EventRegistration.create(
                new EventRegistration.CreateEventRegistration(memberId, siCardNumber));
        registrations.add(registration);

        // Register domain event
        registerEvent(MemberRegisteredForEventEvent.fromAggregate(this, memberId));
    }

    /**
     * Unregister a member from this event.
     * <p>
     * Business rule: Unregistration is only allowed before the event date and before the registration deadline.
     *
     * @param command unregister command containing the member ID
     * @throws BusinessRuleViolationException if registration deadline has passed or event date is today/past
     * @throws IllegalArgumentException       if member is not registered
     */
    public void unregisterMember(UnregisterMember command) {
        LocalDate today = LocalDate.now();

        if (registrationDeadline != null && !registrationDeadline.isAfter(today)) {
            throw new BusinessRuleViolationException("Registration deadline has passed") {};
        }

        if (!today.isBefore(eventDate)) {
            throw new BusinessRuleViolationException("Cannot unregister on or after event date") {
            };
        }

        EventRegistration registration = findRegistration(command.memberId())
                .orElseThrow(() -> new IllegalArgumentException("Member is not registered for this event"));

        registrations.remove(registration);

        registerEvent(MemberUnregisteredFromEventEvent.fromAggregate(this, command.memberId()));
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
