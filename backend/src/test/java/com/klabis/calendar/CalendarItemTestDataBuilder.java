package com.klabis.calendar;

import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.domain.EventCalendarItem;
import com.klabis.calendar.domain.ManualCalendarItem;
import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.EventId;

import java.time.LocalDate;
import java.util.UUID;

public class CalendarItemTestDataBuilder {

    private String name = "Test Calendar Item";
    private String description = "Test calendar item description";
    private LocalDate startDate = LocalDate.of(2026, 3, 15);
    private LocalDate endDate = LocalDate.of(2026, 3, 15);
    private CalendarItemId calendarItemId = new CalendarItemId(UUID.randomUUID());
    private EventId eventId = null;
    private AuditMetadata auditMetadata = null;

    private CalendarItemTestDataBuilder() {
    }

    public static CalendarItemTestDataBuilder aCalendarItem() {
        return new CalendarItemTestDataBuilder();
    }

    public static CalendarItemTestDataBuilder aCalendarItemWithId(CalendarItemId calendarItemId) {
        CalendarItemTestDataBuilder result = aCalendarItem();
        result.calendarItemId = calendarItemId;
        return result;
    }

    public CalendarItemTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public CalendarItemTestDataBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public CalendarItemTestDataBuilder withStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public CalendarItemTestDataBuilder withEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public CalendarItemTestDataBuilder withEventId(EventId eventId) {
        this.eventId = eventId;
        return this;
    }

    public CalendarItemTestDataBuilder withEventId(UUID eventId) {
        this.eventId = new EventId(eventId);
        return this;
    }

    public CalendarItemTestDataBuilder withAuditMetadata(AuditMetadata auditMetadata) {
        this.auditMetadata = auditMetadata;
        return this;
    }

    public CalendarItem build() {
        if (eventId != null) {
            return buildEventLinked(eventId.value());
        }
        return buildManual();
    }

    public ManualCalendarItem buildManual() {
        return ManualCalendarItem.reconstruct(
                calendarItemId,
                name,
                description,
                startDate,
                endDate,
                auditMetadata);
    }

    public EventCalendarItem buildEventLinked(UUID eventIdUuid) {
        EventId eid = new EventId(eventIdUuid);
        return EventCalendarItem.reconstruct(
                calendarItemId,
                name,
                description,
                startDate,
                endDate,
                eid,
                auditMetadata);
    }
}
