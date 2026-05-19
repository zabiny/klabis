package com.klabis.calendar.application;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.domain.CalendarItem;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

@PrimaryPort
public interface CalendarManagementPort {

    List<CalendarItem> listCalendarItems(LocalDate startDate, LocalDate endDate, Sort sort,
                                         boolean myScheduleRequested, @Nullable MemberId myScheduleMemberId);

    CalendarItem getCalendarItem(CalendarItemId calendarItemId);

    CalendarItem createCalendarItem(CalendarItem.CreateCalendarItem command);

    void updateCalendarItem(CalendarItemId calendarItemId, CalendarItem.UpdateCalendarItem command);

    void deleteCalendarItem(CalendarItemId calendarItemId);
}
