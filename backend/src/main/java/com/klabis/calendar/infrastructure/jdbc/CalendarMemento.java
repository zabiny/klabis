package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.CalendarItemId;
import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.EventId;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Memento pattern implementation for CalendarItem aggregate persistence.
 * <p>
 * This class acts as a bridge between the pure domain {@link CalendarItem} entity
 * and Spring Data JDBC persistence. It contains:
 * <ul>
 *   <li>All JDBC annotations for persistence</li>
 *   <li>Flat primitive fields matching the database schema</li>
 *   <li>Conversion methods to/from CalendarItem</li>
 * </ul>
 * <p>
 * The CalendarItem entity remains a pure domain object without Spring annotations,
 * while this memento handles all infrastructure concerns.
 */
@Table("calendar_items")
class CalendarMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("start_date")
    private LocalDate startDate;

    @Column("end_date")
    private LocalDate endDate;

    @Column("event_id")
    private UUID eventId;

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
    @Column("last_modified_by")
    private String lastModifiedBy;

    @Version
    @Column("version")
    private Long version;

    // Transient flag for Persistable<UUID>
    @Transient
    private boolean isNew = true;

    protected CalendarMemento() {
    }

    /**
     * Creates a CalendarMemento from a CalendarItem entity (for save operations).
     *
     * @param calendarItem the CalendarItem entity to convert
     * @return a new CalendarMemento with all fields copied from the CalendarItem
     */
    static CalendarMemento from(CalendarItem calendarItem) {
        Assert.notNull(calendarItem, "CalendarItem must not be null");

        CalendarMemento memento = new CalendarMemento();

        memento.id = calendarItem.getId() != null ? calendarItem.getId().value() : null;
        memento.name = calendarItem.getName();
        memento.description = calendarItem.getDescription();
        memento.startDate = calendarItem.getStartDate();
        memento.endDate = calendarItem.getEndDate();
        memento.eventId = calendarItem.getEventId() != null ? calendarItem.getEventId().value() : null;

        copyAuditMetadata(calendarItem, memento);

        memento.isNew = (calendarItem.getAuditMetadata() == null);

        return memento;
    }

    private static void copyAuditMetadata(CalendarItem calendarItem, CalendarMemento memento) {
        AuditMetadata auditMetadata = calendarItem.getAuditMetadata();
        if (auditMetadata != null) {
            memento.createdAt = auditMetadata.createdAt();
            memento.createdBy = auditMetadata.createdBy();
            memento.lastModifiedAt = auditMetadata.lastModifiedAt();
            memento.lastModifiedBy = auditMetadata.lastModifiedBy();
            memento.version = auditMetadata.version();
        }
    }

    /**
     * Converts this CalendarMemento to a CalendarItem entity (for load operations).
     *
     * @return a reconstituted CalendarItem entity
     */
    CalendarItem toCalendarItem() {
        CalendarItemId calendarItemId = new CalendarItemId(this.id);
        EventId eventIdObj = this.eventId != null ? new EventId(this.eventId) : null;

        AuditMetadata auditMetadata = null;
        if (this.createdAt != null) {
            auditMetadata = new AuditMetadata(
                    this.createdAt,
                    this.createdBy,
                    this.lastModifiedAt,
                    this.lastModifiedBy,
                    this.version);
        }

        return CalendarItem.reconstruct(
                calendarItemId,
                this.name,
                this.description,
                this.startDate,
                this.endDate,
                eventIdObj,
                auditMetadata
        );
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
