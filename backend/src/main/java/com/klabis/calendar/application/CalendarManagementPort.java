package com.klabis.calendar.application;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.domain.CalendarItem;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.data.domain.Pageable;

import java.util.List;

@PrimaryPort
public interface CalendarManagementPort {

    List<CalendarItem> listCalendarItems(CalendarFilter filter, Pageable pageable);

    CalendarItem getCalendarItem(CalendarItemId calendarItemId);

    CalendarItem createCalendarItem(CalendarItem.CreateCalendarItem command);

    void updateCalendarItem(CalendarItemId calendarItemId, CalendarItem.UpdateCalendarItem command);

    void deleteCalendarItem(CalendarItemId calendarItemId);
}
