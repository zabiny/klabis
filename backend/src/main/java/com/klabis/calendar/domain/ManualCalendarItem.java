package com.klabis.calendar.domain;

import com.klabis.calendar.CalendarItemId;
import com.klabis.common.domain.AuditMetadata;

import java.time.LocalDate;

/**
 * A calendar item created, updated and deleted directly by users with CALENDAR:MANAGE authority.
 * <p>
 * Manual items have no event association and are fully editable.
 */
public class ManualCalendarItem extends CalendarItem {

    ManualCalendarItem(
            CalendarItemId id,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate) {

        super(id, name, description, startDate, endDate);
    }

    public static ManualCalendarItem create(CreateCalendarItem command) {
        validateName(command.name());
        validateStartDate(command.startDate());
        validateEndDate(command.endDate());
        validateDateRange(command.startDate(), command.endDate());

        return new ManualCalendarItem(
                CalendarItemId.generate(),
                command.name(),
                command.description(),
                command.startDate(),
                command.endDate());
    }

    public static ManualCalendarItem reconstruct(
            CalendarItemId id,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            AuditMetadata auditMetadata) {

        ManualCalendarItem item = new ManualCalendarItem(id, name, description, startDate, endDate);
        item.updateAuditMetadata(auditMetadata);
        return item;
    }

    public void update(UpdateCalendarItem command) {
        validateName(command.name());
        validateStartDate(command.startDate());
        validateEndDate(command.endDate());
        validateDateRange(command.startDate(), command.endDate());

        this.name = command.name();
        this.description = command.description();
        this.startDate = command.startDate();
        this.endDate = command.endDate();
    }

    @Override
    public void assertCanBeDeleted() {
        // Manual items can always be deleted — no guard needed
    }
}
