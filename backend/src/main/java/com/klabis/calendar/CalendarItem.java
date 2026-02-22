package com.klabis.calendar;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Association;
import org.jmolecules.ddd.annotation.Identity;

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
     * Use {@link #create(String, String, LocalDate, LocalDate)} for creating new manual items.
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

        CalendarItem calendarItem = new CalendarItem(
                id,
                name,
                description,
                startDate,
                endDate,
                eventId,
                auditMetadata
        );
        return calendarItem;
    }

    /**
     * Static factory method to create a new manual CalendarItem.
     * <p>
     * Creates a new manual calendar item (eventId = null) with a generated ID.
     * Manual items can be updated and deleted by authorized users.
     *
     * @param name        calendar item name (required)
     * @param description calendar item description (required)
     * @param startDate   start date (required)
     * @param endDate     end date (required, must be >= startDate)
     * @return new CalendarItem instance
     * @throws IllegalArgumentException if business rules are violated
     */
    public static CalendarItem create(
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate) {

        validateName(name);
        validateDescription(description);
        validateStartDate(startDate);
        validateEndDate(endDate);
        validateDateRange(startDate, endDate);

        return new CalendarItem(
                CalendarItemId.generate(),
                name,
                description,
                startDate,
                endDate,
                null,
                AuditMetadata.create("system")
        );
    }

    // ========== Validation Methods ==========

    private static void validateName(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("Calendar item name is required");
        }
    }

    private static void validateDescription(String description) {
        if (description == null || description.trim().isBlank()) {
            throw new IllegalArgumentException("Calendar item description is required");
        }
    }

    private static void validateStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }
    }

    private static void validateEndDate(LocalDate endDate) {
        if (endDate == null) {
            throw new IllegalArgumentException("End date is required");
        }
    }

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }
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
     * @param name        new calendar item name (required)
     * @param description new calendar item description (required)
     * @param startDate   new start date (required)
     * @param endDate     new end date (required, must be >= startDate)
     * @throws BusinessRuleViolationException if calendar item is event-linked (read-only)
     * @throws IllegalArgumentException       if validation fails
     */
    public void update(
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate) {

        if (isEventLinked()) {
            throw new BusinessRuleViolationException("Cannot manually update event-linked calendar item") {
            };
        }

        validateName(name);
        validateDescription(description);
        validateStartDate(startDate);
        validateEndDate(endDate);
        validateDateRange(startDate, endDate);

        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Deletes the calendar item.
     * <p>
     * Business rule: Only manual items (eventId == null) can be deleted.
     * Event-linked items are read-only and managed automatically via event handlers.
     *
     * @throws BusinessRuleViolationException if calendar item is event-linked (read-only)
     */
    public void delete() {
        if (isEventLinked()) {
            throw new BusinessRuleViolationException("Cannot manually delete event-linked calendar item") {
            };
        }
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
