package com.klabis.events.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.*;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;
/**
 * Event aggregate root.
 * <p>
 * Represents an orienteering event with details about name, date, location, and coordinator.
 * This is the aggregate root for the Events bounded context.
 * <p>
 * Business invariants:
 * - Name, event date, and organizer are required
 * - Location is optional; null means the location is not known
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
    private LinkedHashSet<MemberId> coordinators;
    @Association
    private EventTypeId eventTypeId;
    private RegistrationDeadlines registrationDeadlines;
    private EventStatus status;
    private String cancellationReason;
    private List<EventCategory> categories = new ArrayList<>();
    private EventRanking ranking;
    private Money baseEntryFee;
    private boolean sharedTransportEnabled;
    private boolean sharedAccommodationEnabled;
    private SiPolicy siPolicy;

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

            @Size(max = 100, message = "Event location must not exceed 100 characters")
            String location,

            @NotBlank(message = "Event organizer is required")
            @Size(max = 10, message = "Event organizer must not exceed 10 characters")
            String organizer,

            @URL(message = "Website URL must be valid")
            String websiteUrl,

            LinkedHashSet<MemberId> coordinators,
            EventTypeId eventTypeId,
            RegistrationDeadlines registrationDeadlines,
            List<EventCategory> categories,
            Boolean sharedTransportEnabled,
            Boolean sharedAccommodationEnabled,
            SiPolicy siPolicy
    ) {
        public CreateEvent(String name, LocalDate eventDate, String location, String organizer, String websiteUrl,
                           LinkedHashSet<MemberId> coordinators, EventTypeId eventTypeId,
                           RegistrationDeadlines registrationDeadlines, List<EventCategory> categories) {
            this(name, eventDate, location, organizer, websiteUrl, coordinators, eventTypeId, registrationDeadlines,
                    categories, false, false, null);
        }

        public CreateEvent(String name, LocalDate eventDate, String location, String organizer, String websiteUrl,
                           LinkedHashSet<MemberId> coordinators, EventTypeId eventTypeId,
                           RegistrationDeadlines registrationDeadlines, List<EventCategory> categories,
                           Boolean sharedTransportEnabled, Boolean sharedAccommodationEnabled) {
            this(name, eventDate, location, organizer, websiteUrl, coordinators, eventTypeId, registrationDeadlines,
                    categories, sharedTransportEnabled, sharedAccommodationEnabled, null);
        }

        public static CreateEvent from(Event event) {
            return new CreateEvent(
                    event.name,
                    event.eventDate,
                    event.location,
                    event.organizer,
                    event.websiteUrl != null ? event.websiteUrl.value() : null,
                    new LinkedHashSet<>(event.coordinators),
                    event.eventTypeId,
                    event.registrationDeadlines,
                    event.categories,
                    event.sharedTransportEnabled,
                    event.sharedAccommodationEnabled,
                    event.siPolicy
            );
        }
    }

    @RecordBuilder
    public record UpdateEvent(
            @NotBlank(message = "Event name is required")
            @Size(max = 100, message = "Event name must not exceed 100 characters")
            String name,

            @NotNull(message = "Event date is required")
            LocalDate eventDate,

            @Size(max = 100, message = "Event location must not exceed 100 characters")
            String location,

            @NotBlank(message = "Event organizer is required")
            @Size(max = 10, message = "Event organizer must not exceed 10 characters")
            String organizer,

            @URL(message = "Website URL must be valid")
            String websiteUrl,

            LinkedHashSet<MemberId> coordinators,
            EventTypeId eventTypeId,

            RegistrationDeadlines registrationDeadlines,
            List<EventCategory> categories,
            EventRanking ranking,
            Money baseEntryFee,
            Boolean sharedTransportEnabled,
            Boolean sharedAccommodationEnabled,
            SiPolicy siPolicy
    ) {
        public static UpdateEvent from(Event event) {
            return new UpdateEvent(
                    event.name,
                    event.eventDate,
                    event.location,
                    event.organizer,
                    event.websiteUrl != null ? event.websiteUrl.value() : null,
                    new LinkedHashSet<>(event.coordinators),
                    event.eventTypeId,
                    event.registrationDeadlines,
                    event.categories,
                    event.ranking,
                    event.baseEntryFee,
                    event.sharedTransportEnabled,
                    event.sharedAccommodationEnabled,
                    event.siPolicy
            );
        }
    }

    @RecordBuilder
    public record CreateEventFromOris(
            String name,
            LocalDate eventDate,
            String location,
            String organizer,
            WebsiteUrl websiteUrl,
            RegistrationDeadlines registrationDeadlines,
            List<EventCategory> categories,
            EventRanking ranking,
            Money baseEntryFee
    ) {
    }

    @RecordBuilder
    public record RegisterCommand(
            String siCardNumber,
            EventCategoryId categoryId,
            boolean wantsSharedTransport,
            boolean wantsSharedAccommodation,
            RegistrationNote internalNote,
            RegistrationNote orisNote,
            boolean wantsToRentSi
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
        public static UnregisterMember from(Event event) {
            return new UnregisterMember(null);
        }
    }

    @RecordBuilder
    public record SyncFromOris(
            String name,
            LocalDate eventDate,
            String location,
            String organizer,
            WebsiteUrl websiteUrl,
            RegistrationDeadlines registrationDeadlines,
            List<EventCategory> categories,
            EventRanking ranking,
            Money baseEntryFee
    ) {
        public static SyncFromOris from(Event event) {
            return new SyncFromOris(
                    event.name,
                    event.eventDate,
                    event.location,
                    event.organizer,
                    event.websiteUrl,
                    event.registrationDeadlines,
                    event.categories,
                    event.ranking,
                    event.baseEntryFee
            );
        }
    }

    @RecordBuilder
    public record CancelEvent(
            @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
            String cancellationReason
    ) {
        public CancelEvent {
            if (cancellationReason != null && cancellationReason.length() > 500) {
                throw new IllegalArgumentException("Cancellation reason must not exceed 500 characters");
            }
        }

        public static CancelEvent withoutReason() {
            return new CancelEvent(null);
        }

        public Optional<String> reason() {
            return Optional.ofNullable(cancellationReason);
        }
    }

    @RecordBuilder
    public record EditRegistrationCommand(
            SiCardNumber siCardNumber,
            EventCategoryId categoryId,
            boolean wantsSharedTransport,
            boolean wantsSharedAccommodation,
            RegistrationNote internalNote,
            RegistrationNote orisNote,
            boolean wantsToRentSi
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
            LinkedHashSet<MemberId> coordinators,
            EventTypeId eventTypeId,
            RegistrationDeadlines registrationDeadlines,
            EventStatus status,
            String cancellationReason,
            List<EventCategory> categories,
            EventRanking ranking,
            Money baseEntryFee,
            boolean sharedTransportEnabled,
            boolean sharedAccommodationEnabled,
            SiPolicy siPolicy,
            AuditMetadata auditMetadata) {

        this.id = id;
        this.name = name;
        this.eventDate = eventDate;
        this.location = location;
        this.organizer = organizer;
        this.websiteUrl = websiteUrl;
        this.coordinators = coordinators != null ? new LinkedHashSet<>(coordinators) : new LinkedHashSet<>();
        this.eventTypeId = eventTypeId;
        this.registrationDeadlines = registrationDeadlines != null ? registrationDeadlines : RegistrationDeadlines.none();
        this.status = status;
        this.cancellationReason = cancellationReason;
        this.categories = categories != null ? new ArrayList<>(categories) : new ArrayList<>();
        this.ranking = ranking;
        this.baseEntryFee = baseEntryFee;
        this.sharedTransportEnabled = sharedTransportEnabled;
        this.sharedAccommodationEnabled = sharedAccommodationEnabled;
        this.siPolicy = siPolicy;
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
     * @param coordinators       event coordinators (may be empty)
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
            LinkedHashSet<MemberId> coordinators,
            EventTypeId eventTypeId,
            RegistrationDeadlines registrationDeadlines,
            EventStatus status,
            String cancellationReason,
            List<EventCategory> categories,
            EventRanking ranking,
            Money baseEntryFee,
            List<EventRegistration> registrations,
            AuditMetadata auditMetadata) {
        return reconstruct(id, name, eventDate, location, organizer, websiteUrl, coordinators, eventTypeId,
                registrationDeadlines, status, cancellationReason, categories, ranking, baseEntryFee,
                false, false, registrations, auditMetadata);
    }

    public static Event reconstruct(
            EventId id,
            String name,
            LocalDate eventDate,
            String location,
            String organizer,
            WebsiteUrl websiteUrl,
            LinkedHashSet<MemberId> coordinators,
            EventTypeId eventTypeId,
            RegistrationDeadlines registrationDeadlines,
            EventStatus status,
            String cancellationReason,
            List<EventCategory> categories,
            EventRanking ranking,
            Money baseEntryFee,
            boolean sharedTransportEnabled,
            boolean sharedAccommodationEnabled,
            List<EventRegistration> registrations,
            AuditMetadata auditMetadata) {
        return reconstruct(id, name, eventDate, location, organizer, websiteUrl, coordinators, eventTypeId,
                registrationDeadlines, status, cancellationReason, categories, ranking, baseEntryFee,
                sharedTransportEnabled, sharedAccommodationEnabled, SiPolicy.defaultPolicy(), registrations,
                auditMetadata);
    }

    public static Event reconstruct(
            EventId id,
            String name,
            LocalDate eventDate,
            String location,
            String organizer,
            WebsiteUrl websiteUrl,
            LinkedHashSet<MemberId> coordinators,
            EventTypeId eventTypeId,
            RegistrationDeadlines registrationDeadlines,
            EventStatus status,
            String cancellationReason,
            List<EventCategory> categories,
            EventRanking ranking,
            Money baseEntryFee,
            boolean sharedTransportEnabled,
            boolean sharedAccommodationEnabled,
            SiPolicy siPolicy,
            List<EventRegistration> registrations,
            AuditMetadata auditMetadata) {

        Event event = new Event(
                id,
                name,
                eventDate,
                location,
                organizer,
                websiteUrl,
                coordinators,
                eventTypeId,
                registrationDeadlines,
                status,
                cancellationReason,
                categories,
                ranking,
                baseEntryFee,
                sharedTransportEnabled,
                sharedAccommodationEnabled,
                siPolicy,
                auditMetadata
        );
        event.registrations.addAll(registrations);
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
        validateOrganizer(command.organizer());
        RegistrationDeadlines deadlines = command.registrationDeadlines() != null
                ? command.registrationDeadlines() : RegistrationDeadlines.none();
        validateDeadlinesAgainstEventDate(deadlines, command.eventDate());
        validateCategories(command.categories());

        Event event = new Event(
                EventId.generate(),
                command.name(),
                command.eventDate(),
                command.location(),
                command.organizer(),
                command.websiteUrl() != null ? WebsiteUrl.of(command.websiteUrl()) : null,
                command.coordinators(),
                command.eventTypeId(),
                deadlines,
                EventStatus.DRAFT,
                null,
                command.categories(),
                null,
                null,
                Boolean.TRUE.equals(command.sharedTransportEnabled()),
                Boolean.TRUE.equals(command.sharedAccommodationEnabled()),
                command.siPolicy() != null ? command.siPolicy() : SiPolicy.defaultPolicy(),
                null
        );

        event.registerEvent(EventCreatedEvent.fromAggregate(event));

        return event;
    }

    /**
     * Factory method to create an Event imported from ORIS.
     * <p>
     * Creates a new event in DRAFT status with data sourced from the ORIS orienteering system.
     *
     * @param command event creation command with all ORIS-sourced fields
     * @return new Event instance in DRAFT status
     */
    public static Event createFromOris(CreateEventFromOris command) {
        validateName(command.name());
        validateEventDate(command.eventDate());
        validateOrganizer(command.organizer());
        RegistrationDeadlines deadlines = command.registrationDeadlines() != null
                ? command.registrationDeadlines() : RegistrationDeadlines.none();
        validateDeadlinesAgainstEventDate(deadlines, command.eventDate());
        validateCategories(command.categories());

        Event event = new Event(
                EventId.generate(),
                command.name(),
                command.eventDate(),
                command.location(),
                command.organizer(),
                command.websiteUrl(),
                new LinkedHashSet<>(),
                null,
                deadlines,
                EventStatus.DRAFT,
                null,
                command.categories(),
                command.ranking(),
                command.baseEntryFee(),
                false,
                false,
                SiPolicy.defaultPolicy(),
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

    private static void validateOrganizer(String organizer) {
        if (organizer == null || organizer.trim().isBlank()) {
            throw new IllegalArgumentException("Event organizer is required");
        }
    }

    private static void validateCategories(List<EventCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }
        Set<String> seenNames = new HashSet<>();
        Set<String> seenOrisIds = new HashSet<>();
        for (EventCategory category : categories) {
            if (!seenNames.add(category.name())) {
                throw new BusinessRuleViolationException(
                        "Category name '" + category.name() + "' is used more than once") {};
            }
            if (category.orisId() != null && !seenOrisIds.add(category.orisId())) {
                throw new BusinessRuleViolationException(
                        "Category orisId '" + category.orisId() + "' is used more than once") {};
            }
        }
    }

    private static void validateDeadlinesAgainstEventDate(RegistrationDeadlines deadlines, LocalDate eventDate) {
        if (deadlines == null || eventDate == null) {
            return;
        }
        deadlines.last().ifPresent(last -> {
            if (last.isAfter(eventDate)) {
                throw new BusinessRuleViolationException("Registration deadline must be on or before event date") {};
            }
        });
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

    public Set<MemberId> getCoordinators() {
        return Collections.unmodifiableSet(coordinators);
    }

    public boolean isCoordinator(MemberId memberId) {
        return coordinators.contains(memberId);
    }

    public Optional<EventTypeId> getEventTypeId() {
        return Optional.ofNullable(eventTypeId);
    }

    public RegistrationDeadlines getRegistrationDeadlines() {
        return registrationDeadlines;
    }

    public EventStatus getStatus() {
        return status;
    }

    public List<EventCategory> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    public Optional<String> getCancellationReason() {
        return Optional.ofNullable(cancellationReason);
    }

    public EventRanking getRanking() {
        return ranking;
    }

    public Money getBaseEntryFee() {
        return baseEntryFee;
    }

    public SiPolicy getSiPolicy() {
        return siPolicy;
    }

    public boolean isSharedTransportEnabled() {
        return sharedTransportEnabled;
    }

    public boolean isSharedAccommodationEnabled() {
        return sharedAccommodationEnabled;
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
     * An optional cancellation reason (max 500 chars) may be provided to communicate context to members.
     *
     * @param command cancel command with optional reason
     * @throws IllegalStateException if transition is not allowed
     */
    public void cancel(CancelEvent command) {
        status.validateTransition(EventStatus.CANCELLED);
        this.status = EventStatus.CANCELLED;
        this.cancellationReason = command.cancellationReason();

        // Register domain event
        registerEvent(EventCancelledEvent.fromAggregate(this));
    }

    /**
     * Cancels the event without a reason. Convenience overload.
     */
    public void cancel() {
        cancel(CancelEvent.withoutReason());
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
        validateOrganizer(command.organizer());
        RegistrationDeadlines deadlines = command.registrationDeadlines() != null
                ? command.registrationDeadlines() : RegistrationDeadlines.none();
        validateDeadlinesAgainstEventDate(deadlines, command.eventDate());
        validateCategories(command.categories());

        this.name = command.name();
        this.eventDate = command.eventDate();
        this.location = command.location();
        this.organizer = command.organizer();
        this.websiteUrl = command.websiteUrl() != null ? WebsiteUrl.of(command.websiteUrl()) : null;
        this.coordinators = command.coordinators() != null ? new LinkedHashSet<>(command.coordinators()) : new LinkedHashSet<>();
        this.eventTypeId = command.eventTypeId();
        this.registrationDeadlines = deadlines;
        this.categories = command.categories() != null ? new ArrayList<>(command.categories()) : new ArrayList<>();
        this.ranking = command.ranking();
        this.baseEntryFee = command.baseEntryFee();
        this.sharedTransportEnabled = Boolean.TRUE.equals(command.sharedTransportEnabled());
        this.sharedAccommodationEnabled = Boolean.TRUE.equals(command.sharedAccommodationEnabled());
        // An absent policy means "unchanged", like the PATCH null semantics of siUsage.
        if (command.siPolicy() != null) {
            this.siPolicy = command.siPolicy();
        }

        registerEvent(EventUpdatedEvent.fromAggregate(this, UpdateOrigin.MANUAL));
    }

    /**
     * Syncs event data from ORIS, overwriting all relevant fields.
     * <p>
     * Allowed in any status, including FINISHED and CANCELLED: the synchronisation
     * engine retires the pairing for those the moment the event reaches the end of
     * its life (see {@code EventsSyncListener}), so a scheduled pass never reaches
     * this method for them — the only way it does is a manager deliberately
     * reactivating the pairing by importing the same ORIS event again (design.md D7,
     * D6). Reactivation resumes synchronisation of data only; it does not reopen the
     * event, so a FINISHED or CANCELLED event keeps its status.
     * <p>
     * Whether an event should be synced from ORIS at all is decided by the {@code sync}
     * module's enrolment (a {@code SyncRecord} pairing) before this method is ever
     * invoked — see {@code OrisEventSyncAdapter.applyToLocal}.
     *
     * @param command sync command with all ORIS-sourced fields
     */
    public void syncFromOris(SyncFromOris command) {
        validateCategories(command.categories());

        this.name = command.name();
        this.eventDate = command.eventDate();
        this.location = command.location();
        this.organizer = command.organizer();
        this.websiteUrl = command.websiteUrl();
        this.registrationDeadlines = command.registrationDeadlines() != null
                ? command.registrationDeadlines() : RegistrationDeadlines.none();
        this.categories = mergeCategoriesFromOris(command.categories());
        this.ranking = command.ranking();
        this.baseEntryFee = command.baseEntryFee();

        registerEvent(EventUpdatedEvent.fromAggregate(this, UpdateOrigin.SYNCHRONISATION));
    }

    /**
     * Merges incoming ORIS categories with the event's current categories, matching by {@code orisId}.
     * <p>
     * A match keeps the existing category's local id (so registrations stay attached) and adopts the
     * incoming name. Categories with {@code orisId == null} were added manually and are never touched by
     * a sync — they are not part of the ORIS source, so ORIS cannot decide to remove them. Existing
     * ORIS-origin categories whose {@code orisId} is absent from the incoming data are dropped.
     */
    private List<EventCategory> mergeCategoriesFromOris(List<EventCategory> incomingCategories) {
        List<EventCategory> incoming = incomingCategories != null ? incomingCategories : List.of();
        Map<String, EventCategory> existingByOrisId = this.categories.stream()
                .filter(c -> c.orisId() != null)
                .collect(Collectors.toMap(EventCategory::orisId, c -> c));

        List<EventCategory> merged = new ArrayList<>();
        for (EventCategory incomingCategory : incoming) {
            EventCategory existing = existingByOrisId.get(incomingCategory.orisId());
            if (existing != null) {
                merged.add(new EventCategory(existing.id(), existing.orisId(), incomingCategory.name(), existing.feeOverride()));
            } else {
                merged.add(incomingCategory);
            }
        }

        this.categories.stream()
                .filter(c -> c.orisId() == null)
                .forEach(merged::add);

        return merged;
    }

    /**
     * Fills in the event type from ORIS auto-mapping only when no type has been set yet.
     * Preserves any type already present — whether set automatically on a previous sync or manually by a user.
     */
    public void applyAutoMappedEventType(EventTypeId resolvedTypeId) {
        if (this.eventTypeId == null && resolvedTypeId != null) {
            this.eventTypeId = resolvedTypeId;
        }
    }

    /**
     * Returns true when registrations are open: the event is ACTIVE, the event date is strictly in the future,
     * and the registration deadline (if set) has not passed.
     */
    public boolean areRegistrationsOpen() {
        return registrationsOpenViolation().isEmpty();
    }

    // ========== Registration Methods ==========

    /**
     * Register a member for this event.
     * <p>
     * Business rules:
     * - Registration is only allowed for ACTIVE events with open registration window
     * - Member cannot be registered twice for the same event (domain invariant)
     * - When event has categories, a category MUST be provided and MUST be in the event's category list
     * - When event has no categories, category is ignored
     *
     * @param memberId     member's user ID (required)
     * @param command      registration data; category is required when event has categories, ignored otherwise
     * @throws BusinessRuleViolationException    if registrations are not open or category validation fails
     * @throws DuplicateRegistrationException    if member is already registered
     */
    public void registerMember(MemberId memberId, RegisterCommand command) {
        assertRegistrationsOpen();

        if (findRegistration(memberId).isPresent()) {
            throw new DuplicateRegistrationException(memberId, this.id);
        }

        // Values the policy does not offer are not taken, so they are not even parsed.
        SiCardNumber siCardNumber = siPolicy.usage() != SiUsage.NOT_USED && StringUtils.hasText(command.siCardNumber())
                ? SiCardNumber.of(command.siCardNumber()) : null;
        siPolicy.validate(siCardNumber, command.wantsToRentSi());
        boolean wantsToRentSi = siPolicy.rentalAllowed() && command.wantsToRentSi();

        EventRegistration registration = EventRegistration.create(
                new EventRegistration.CreateEventRegistration(memberId, siCardNumber,
                        resolveCategoryId(command.categoryId()),
                        command.wantsSharedTransport(), command.wantsSharedAccommodation(),
                        command.internalNote(), command.orisNote(), wantsToRentSi));
        registrations.add(registration);

        registerEvent(MemberRegisteredForEventEvent.fromAggregate(this, memberId));
    }

    private EventCategoryId resolveCategoryId(EventCategoryId categoryId) {
        if (categories.isEmpty()) {
            return null;
        }
        if (categoryId == null) {
            throw new BusinessRuleViolationException("Category is required for this event") {};
        }
        boolean isKnownCategory = categories.stream().anyMatch(c -> c.id().equals(categoryId));
        if (!isKnownCategory) {
            throw new BusinessRuleViolationException(
                    "Category '" + categoryId + "' is not available for this event") {};
        }
        return categoryId;
    }

    /**
     * Unregister a member from this event.
     * <p>
     * Business rule: Unregistration is only allowed when registrations are open.
     *
     * @param command unregister command containing the member ID
     * @throws BusinessRuleViolationException    if registrations are not open
     * @throws RegistrationNotFoundException     if member is not registered
     */
    public void unregisterMember(UnregisterMember command) {
        assertRegistrationsOpen();

        EventRegistration registration = findRegistration(command.memberId())
                .orElseThrow(() -> new RegistrationNotFoundException(command.memberId(), this.id));

        registrations.remove(registration);

        registerEvent(MemberUnregisteredFromEventEvent.fromAggregate(this, command.memberId()));
    }

    public void editRegistration(MemberId memberId, EditRegistrationCommand command) {
        assertRegistrationsOpen();

        EventRegistration current = findRegistration(memberId)
                .orElseThrow(() -> new RegistrationNotFoundException(memberId, this.id));

        EventCategoryId resolvedCategoryId = resolveCategoryId(command.categoryId());
        boolean wantsSharedTransport = sharedTransportEnabled ? command.wantsSharedTransport()
                : current.wantsSharedTransport();
        boolean wantsSharedAccommodation = sharedAccommodationEnabled ? command.wantsSharedAccommodation()
                : current.wantsSharedAccommodation();

        // Values the policy does not offer keep their current value, like the shared-service offers.
        SiCardNumber siCardNumber = siPolicy.usage() == SiUsage.NOT_USED ? current.siCardNumber()
                : command.siCardNumber();
        boolean wantsToRentSi = siPolicy.rentalAllowed() ? command.wantsToRentSi() : current.wantsToRentSi();
        // A rental flag kept because rental is not offered must not block the edit.
        siPolicy.validate(siCardNumber, siPolicy.rentalAllowed() && wantsToRentSi);

        EventRegistration updated = current.withChanges(EventEditRegistrationCommandBuilder.builder(command)
                .siCardNumber(siCardNumber)
                .wantsToRentSi(wantsToRentSi)
                .categoryId(resolvedCategoryId)
                .wantsSharedTransport(wantsSharedTransport)
                .wantsSharedAccommodation(wantsSharedAccommodation)
                .build());

        registrations.remove(current);
        registrations.add(updated);

        registerEvent(RegistrationEditedEvent.fromAggregate(this, memberId, current, updated));
    }

    private void assertRegistrationsOpen() {
        registrationsOpenViolation().ifPresent(message -> {
            throw new BusinessRuleViolationException(message) {};
        });
    }

    private Optional<String> registrationsOpenViolation() {
        if (status != EventStatus.ACTIVE) {
            return Optional.of("Registration is only allowed for ACTIVE events");
        }
        LocalDate today = LocalDate.now();
        if (!registrationDeadlines.registrationsOpen(today)) {
            return Optional.of("Registration deadline has passed");
        }
        if (!today.isBefore(eventDate)) {
            return Optional.of("Cannot register/unregister on or after event date");
        }
        return Optional.empty();
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

    /**
     * Number of registered members who asked to use shared transport. A plain tally over the
     * registrations — it does not consult {@link #isSharedTransportEnabled()}; callers that only
     * want the count for an enabled offer gate on that flag themselves.
     */
    public long sharedTransportCount() {
        return registrations.stream().filter(EventRegistration::wantsSharedTransport).count();
    }

    /**
     * Number of registered members who asked to use shared accommodation. See
     * {@link #sharedTransportCount()} for the gating note.
     */
    public long sharedAccommodationCount() {
        return registrations.stream().filter(EventRegistration::wantsSharedAccommodation).count();
    }

    /**
     * Look up a category by id among this event's current categories.
     * <p>
     * Returns empty when {@code categoryId} is null or no longer present on the event — this is the
     * orphaned-registration case: the id stays on the registration, but the category was removed.
     *
     * @param categoryId category id to resolve, may be null
     * @return the matching category, or empty if not found
     */
    public Optional<EventCategory> findCategory(EventCategoryId categoryId) {
        if (categoryId == null) {
            return Optional.empty();
        }
        return categories.stream().filter(c -> c.id().equals(categoryId)).findFirst();
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
