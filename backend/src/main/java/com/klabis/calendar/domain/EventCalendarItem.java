package com.klabis.calendar.domain;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.CalendarItemKind;
import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.EventData;
import com.klabis.events.EventId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.ddd.annotation.Association;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A calendar item synchronized from an Event.
 * <p>
 * Created automatically when an event is published. Updated when the event is modified.
 * Deleted when the event is cancelled. Read-only from a user perspective — cannot be manually
 * updated or deleted.
 * <p>
 * The {@code kind} field distinguishes between the event-date item ({@link CalendarItemKind#EVENT_DATE})
 * and the registration-deadline item ({@link CalendarItemKind#EVENT_REGISTRATION_DATE}).
 */
public class EventCalendarItem extends CalendarItem {

    @RecordBuilder
    public record CreateCalendarItemForEvent(
            String name,
            String location,
            String organizer,
            String websiteUrl,
            LocalDate eventDate,
            EventId eventId
    ) {
        public String description() {
            return buildEventDescription(location, organizer, websiteUrl);
        }
    }

    private static String buildEventDescription(String location, String organizer, String websiteUrl) {
        List<String> parts = new ArrayList<>();
        if (location != null && !location.isBlank()) parts.add(location);
        if (organizer != null && !organizer.isBlank()) parts.add(organizer);

        String base = parts.isEmpty() ? null : String.join(" - ", parts);

        if (websiteUrl != null && !websiteUrl.isBlank()) {
            return base != null ? base + "\n" + websiteUrl : websiteUrl;
        }
        return base;
    }

    @Association
    private final EventId eventId;

    private final CalendarItemKind kind;

    EventCalendarItem(
            CalendarItemId id,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            EventId eventId,
            CalendarItemKind kind) {

        super(id, name, description, startDate, endDate);
        this.eventId = eventId;
        this.kind = kind;
    }

    @Override
    public void assertCanBeDeleted() {
        throw new CalendarItemReadOnlyException();
    }

    public EventId getEventId() {
        return eventId;
    }

    public CalendarItemKind getKind() {
        return kind;
    }

    public static EventCalendarItem createForEventDate(CreateCalendarItemForEvent command) {
        validateName(command.name());
        validateStartDate(command.eventDate());

        Assert.notNull(command.eventId(), "Event ID is required for event-linked calendar items");

        return new EventCalendarItem(
                CalendarItemId.generate(),
                command.name(),
                command.description(),
                command.eventDate(),
                command.eventDate(),
                command.eventId(),
                CalendarItemKind.EVENT_DATE);
    }

    public static EventCalendarItem createForRegistrationDeadline(
            String eventName, EventId eventId, LocalDate deadlineDate) {

        Assert.hasText(eventName, "Event name is required for registration deadline calendar items");
        Assert.notNull(eventId, "Event ID is required for registration deadline calendar items");
        Assert.notNull(deadlineDate, "Deadline date is required for registration deadline calendar items");

        return new EventCalendarItem(
                CalendarItemId.generate(),
                "Přihlášky - " + eventName,
                null,
                deadlineDate,
                deadlineDate,
                eventId,
                CalendarItemKind.EVENT_REGISTRATION_DATE);
    }

    public static EventCalendarItem reconstruct(
            CalendarItemId id,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            EventId eventId,
            CalendarItemKind kind,
            AuditMetadata auditMetadata) {

        EventCalendarItem item = new EventCalendarItem(id, name, description, startDate, endDate, eventId, kind);
        item.updateAuditMetadata(auditMetadata);
        return item;
    }

    public void synchronizeFromEvent(EventData event) {
        validateName(event.name());

        if (this.kind == CalendarItemKind.EVENT_REGISTRATION_DATE) {
            this.name = "Přihlášky - " + event.name();
            this.description = null;
            this.startDate = event.registrationDeadline();
            this.endDate = event.registrationDeadline();
        } else {
            validateStartDate(event.eventDate());
            this.name = event.name();
            this.description = buildEventDescription(event.location(), event.organizer(), event.websiteUrl());
            this.startDate = event.eventDate();
            this.endDate = event.eventDate();
        }
    }
}
