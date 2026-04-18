package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.domain.EventCalendarItem;
import com.klabis.calendar.domain.ManualCalendarItem;
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

@Table("calendar_items")
class CalendarMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("kind")
    private CalendarItemKind kind;

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

    @Transient
    private boolean isNew = true;

    protected CalendarMemento() {
    }

    static CalendarMemento from(CalendarItem calendarItem) {
        Assert.notNull(calendarItem, "CalendarItem must not be null");

        CalendarMemento memento = new CalendarMemento();

        memento.id = calendarItem.getId() != null ? calendarItem.getId().value() : null;
        memento.name = calendarItem.getName();
        memento.description = calendarItem.getDescription();
        memento.startDate = calendarItem.getStartDate();
        memento.endDate = calendarItem.getEndDate();

        if (calendarItem instanceof ManualCalendarItem) {
            memento.kind = CalendarItemKind.MANUAL;
            memento.eventId = null;
        } else if (calendarItem instanceof EventCalendarItem eventDateItem) {
            memento.kind = CalendarItemKind.EVENT_DATE;
            memento.eventId = eventDateItem.getEventId() != null ? eventDateItem.getEventId().value() : null;
        } else {
            throw new IllegalArgumentException("Unknown CalendarItem subtype: " + calendarItem.getClass().getName());
        }

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

    CalendarItem toCalendarItem() {
        CalendarItemId calendarItemId = new CalendarItemId(this.id);

        AuditMetadata auditMetadata = null;
        if (this.createdAt != null) {
            auditMetadata = new AuditMetadata(
                    this.createdAt,
                    this.createdBy,
                    this.lastModifiedAt,
                    this.lastModifiedBy,
                    this.version);
        }

        if (this.kind == null) {
            throw new IllegalStateException("CalendarItemKind must not be null for persisted item " + this.id);
        }

        return switch (this.kind) {
            case MANUAL -> ManualCalendarItem.reconstruct(
                    calendarItemId,
                    this.name,
                    this.description,
                    this.startDate,
                    this.endDate,
                    auditMetadata);
            case EVENT_DATE -> {
                EventId eventIdObj = this.eventId != null ? new EventId(this.eventId) : null;
                yield EventCalendarItem.reconstruct(
                        calendarItemId,
                        this.name,
                        this.description,
                        this.startDate,
                        this.endDate,
                        eventIdObj,
                        auditMetadata);
            }
        };
    }

    CalendarItemKind kind() {
        return kind;
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
