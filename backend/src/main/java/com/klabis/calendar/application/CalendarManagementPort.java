package com.klabis.calendar.application;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.domain.CalendarItem;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

@PrimaryPort
public interface CalendarManagementPort {

    /**
     * Lists calendar items for the given date range and sort.
     *
     * @param myScheduleMemberId when non-null, restricts results to EVENT_DATE items linked to
     *                           events where this member is an active participant or coordinator;
     *                           when null, returns all items (default behaviour)
     */
    List<CalendarItem> listCalendarItems(@NonNull LocalDate startDate, @NonNull LocalDate endDate, Sort sort,
                                         @Nullable MemberId myScheduleMemberId);

    CalendarItem getCalendarItem(CalendarItemId calendarItemId);

    CalendarItem createCalendarItem(CalendarItem.CreateCalendarItem command);

    void updateCalendarItem(CalendarItemId calendarItemId, CalendarItem.UpdateCalendarItem command);

    void deleteCalendarItem(CalendarItemId calendarItemId);
}
