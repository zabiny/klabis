package com.klabis.calendar.domain;

import com.klabis.calendar.CalendarItemId;
import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.EventId;
import org.jmolecules.ddd.annotation.Association;

import java.time.LocalDate;

/**
 * A calendar item synchronized from an Event.
 * <p>
 * Created automatically when an event is published. Updated when the event is modified.
 * Deleted when the event is cancelled. Read-only from a user perspective — cannot be manually
 * updated or deleted.
 */
public class EventCalendarItem extends CalendarItem {

    @Association
    private final EventId eventId;

    EventCalendarItem(
            CalendarItemId id,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            EventId eventId) {

        super(id, name, description, startDate, endDate);
        this.eventId = eventId;
    }

    @Override
    public void assertCanBeDeleted() {
        throw new CalendarItemReadOnlyException();
    }

    public EventId getEventId() {
        return eventId;
    }

    public static EventCalendarItem createForEvent(CreateCalendarItemForEvent command) {
        validateName(command.name());
        validateStartDate(command.eventDate());

        if (command.eventId() == null) {
            throw new IllegalArgumentException("Event ID is required for event-linked calendar items");
        }

        return new EventCalendarItem(
                CalendarItemId.generate(),
                command.name(),
                command.description(),
                command.eventDate(),
                command.eventDate(),
                command.eventId());
    }

    public static EventCalendarItem reconstruct(
            CalendarItemId id,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            EventId eventId,
            AuditMetadata auditMetadata) {

        EventCalendarItem item = new EventCalendarItem(id, name, description, startDate, endDate, eventId);
        item.updateAuditMetadata(auditMetadata);
        return item;
    }

    public void synchronizeFromEvent(SynchronizeFromEvent command) {
        validateName(command.name());
        validateStartDate(command.eventDate());

        this.name = command.name();
        this.description = command.description();
        this.startDate = command.eventDate();
        this.endDate = command.eventDate();
    }
}
