package com.klabis.calendar.domain;

import com.klabis.calendar.CalendarItemId;
import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.events.EventId;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.Objects;

/**
 * CalendarItem aggregate root.
 * <p>
 * Represents an item on the calendar, either manually created or automatically synchronized from an Event.
 * <p>
 * Business invariants:
 * - Name and description are required (not blank)
 * - End date must be on or after start date
 * - Event-linked items (eventId != null) are read-only and cannot be updated or deleted manually
 * - Manual items (eventId == null) can be updated and deleted
 * <p>
 * Persistence:
 * - This aggregate root is a pure domain object without Spring Data JDBC annotations
 * - ID is stored as UUID in database, exposed as CalendarItemId value object in domain
 * - Value objects are maintained as fields for domain logic
 */
@AggregateRoot
public class CalendarItem extends KlabisAggregateRoot<CalendarItem, CalendarItemId> {

    // ========== Nested Command Records ==========

    @RecordBuilder
    public record CreateCalendarItem(
            @NotBlank(message = "Calendar item name is required")
            @Size(max = 200, message = "Calendar item name must not exceed 200 characters")
            String name,

            @NotBlank(message = "Calendar item description is required")
            @Size(max = 1000, message = "Calendar item description must not exceed 1000 characters")
            String description,

            @NotNull(message = "Start date is required")
            LocalDate startDate,

            @NotNull(message = "End date is required")
            LocalDate endDate
    ) {}

    @RecordBuilder
    public record UpdateCalendarItem(
            @NotBlank(message = "Calendar item name is required")
            @Size(max = 200, message = "Calendar item name must not exceed 200 characters")
            String name,

            @NotBlank(message = "Calendar item description is required")
            @Size(max = 1000, message = "Calendar item description must not exceed 1000 characters")
            String description,

            @NotNull(message = "Start date is required")
            LocalDate startDate,

            @NotNull(message = "End date is required")
            LocalDate endDate
    ) {}

    @RecordBuilder
    public record CreateCalendarItemForEvent(
            String name,
            String location,
            String organizer,
            String websiteUrl,
            LocalDate eventDate,
            EventId eventId
    ) {
        String description() {
            return buildEventDescription(location, organizer, websiteUrl);
        }
    }

    @RecordBuilder
    public record SynchronizeFromEvent(
            String name,
            String location,
            String organizer,
            String websiteUrl,
            LocalDate eventDate
    ) {
        String description() {
            return buildEventDescription(location, organizer, websiteUrl);
        }
    }

    private static String buildEventDescription(String location, String organizer, String websiteUrl) {
        String base = location + " - " + organizer;
        return websiteUrl != null ? base + "\n" + websiteUrl : base;
    }

    @Identity
    private final CalendarItemId id;

    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;

    @Association
    private EventId eventId;

    
    /**
     * Private constructor for creating new CalendarItem instances.
     * <p>
     * This constructor is used by the static factory methods (create, reconstruct)
     * to ensure business invariants are validated during construction.
     */
    private CalendarItem(
            CalendarItemId id,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            EventId eventId,
            AuditMetadata auditMetadata) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.eventId = eventId;
        updateAuditMetadata(auditMetadata);
    }

    /**
     * Factory method for reconstructing CalendarItem from persistence layer.
     * This bypasses validation since the data was already validated when originally stored.
     * <p>
     * This method is public only for infrastructure/persistence layer usage.
     * Use {@link #create(CreateCalendarItem)} for creating new manual items.
     *
     * @param id            calendar item's unique identifier
     * @param name          calendar item name
     * @param description   calendar item description
     * @param startDate     start date
     * @param endDate       end date
     * @param eventId       linked event ID (may be null for manual items)
     * @param auditMetadata audit metadata
     * @return reconstructed CalendarItem instance
     */
    public static CalendarItem reconstruct(
            CalendarItemId id,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            EventId eventId,
            AuditMetadata auditMetadata) {

        return new CalendarItem(id, name, description, startDate, endDate, eventId, auditMetadata);
    }

    /**
     * Static factory method to create a new manual CalendarItem.
     * <p>
     * Creates a new manual calendar item (eventId = null) with a generated ID.
     * Manual items can be updated and deleted by authorized users.
     *
     * @param command creation command with name, description, startDate, endDate
     * @return new CalendarItem instance
     * @throws IllegalArgumentException if business rules are violated
     */
    public static CalendarItem create(CreateCalendarItem command) {
        validateName(command.name());
        validateDescription(command.description());
        validateStartDate(command.startDate());
        validateEndDate(command.endDate());
        validateDateRange(command.startDate(), command.endDate());

        return new CalendarItem(
                CalendarItemId.generate(),
                command.name(),
                command.description(),
                command.startDate(),
                command.endDate(),
                null,
                null
        );
    }

    /**
     * Static factory method to create a new event-linked CalendarItem.
     * <p>
     * Creates a new calendar item synchronized from an Event (eventId != null) with a generated ID.
     * Event-linked items are read-only and can only be updated through event synchronization.
     *
     * @param command creation command with name, description, eventDate, eventId
     * @return new CalendarItem instance
     * @throws IllegalArgumentException if business rules are violated
     */
    public static CalendarItem createForEvent(CreateCalendarItemForEvent command) {
        validateName(command.name());
        validateLocation(command.location());
        validateStartDate(command.eventDate());

        if (command.eventId() == null) {
            throw new IllegalArgumentException("Event ID is required for event-linked calendar items");
        }

        return new CalendarItem(
                CalendarItemId.generate(),
                command.name(),
                command.description(),
                command.eventDate(),
                command.eventDate(),
                command.eventId(),
                null
        );
    }

    // ========== Validation Methods ==========

    private static void validateName(String name) {
        Assert.hasText(name, "Calendar item name is required");
    }

    private static void validateDescription(String description) {
        Assert.hasText(description, "Calendar item description is required");
    }

    private static void validateLocation(String location) {
        Assert.hasText(location, "Calendar item location is required");
    }

    private static void validateStartDate(LocalDate startDate) {
        Assert.notNull(startDate, "Start date is required");
    }

    private static void validateEndDate(LocalDate endDate) {
        Assert.notNull(endDate, "End date is required");
    }

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        Assert.isTrue(!endDate.isBefore(startDate), "End date must be on or after start date");
    }

    // ========== Getters ==========

    public CalendarItemId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public EventId getEventId() {
        return eventId;
    }

    public AuditMetadata getAuditMetadata() {
        return super.getAuditMetadata();
    }

    // ========== Domain Methods ==========

    /**
     * Updates the calendar item details.
     * <p>
     * Business rule: Only manual items (eventId == null) can be updated.
     * Event-linked items are read-only and managed automatically via event handlers.
     *
     * @param command update command with name, description, startDate, endDate
     * @throws CalendarItemReadOnlyException if calendar item is event-linked (read-only)
     * @throws IllegalArgumentException       if validation fails
     */
    public void update(UpdateCalendarItem command) {
        if (isEventLinked()) {
            throw new CalendarItemReadOnlyException();
        }

        validateName(command.name());
        validateDescription(command.description());
        validateStartDate(command.startDate());
        validateEndDate(command.endDate());
        validateDateRange(command.startDate(), command.endDate());

        this.name = command.name();
        this.description = command.description();
        this.startDate = command.startDate();
        this.endDate = command.endDate();
    }

    /**
     * Asserts that this calendar item can be deleted.
     * <p>
     * Business rule: Only manual items (eventId == null) can be deleted.
     * Event-linked items are read-only and managed automatically via event handlers.
     *
     * @throws CalendarItemReadOnlyException if calendar item is event-linked (read-only)
     */
    public void assertCanBeDeleted() {
        if (isEventLinked()) {
            throw new CalendarItemReadOnlyException();
        }
    }

    /**
     * Synchronizes this event-linked calendar item with updated event data.
     * <p>
     * This method is used only by event synchronization infrastructure.
     * This bypasses the manual update protection for event-linked items.
     *
     * @param command synchronization command with name, description, and eventDate
     * @throws IllegalArgumentException if validation fails
     */
    public void synchronizeFromEvent(SynchronizeFromEvent command) {
        validateName(command.name());
        validateLocation(command.location());
        validateStartDate(command.eventDate());
        validateDateRange(command.eventDate(), command.eventDate());

        this.name = command.name();
        this.description = command.description();
        this.startDate = command.eventDate();
        this.endDate = command.eventDate();
    }

    /**
     * Checks if this calendar item is linked to an event.
     * <p>
     * Event-linked items are read-only and cannot be manually updated or deleted.
     *
     * @return true if eventId is not null, false otherwise
     */
    public boolean isEventLinked() {
        return eventId != null;
    }

    // ========== Object Methods ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalendarItem that = (CalendarItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CalendarItem{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", startDate=" + startDate +
               ", endDate=" + endDate +
               ", eventLinked=" + isEventLinked() +
               '}';
    }
}
