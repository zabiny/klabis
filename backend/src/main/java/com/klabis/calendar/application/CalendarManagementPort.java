package com.klabis.calendar.application;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.domain.CalendarItem;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;

@PrimaryPort
public interface CalendarManagementPort {

    List<CalendarItem> listCalendarItems(@NonNull LocalDate startDate, @NonNull LocalDate endDate, Sort sort);

    CalendarItem getCalendarItem(CalendarItemId calendarItemId);

    CalendarItem createCalendarItem(CreateCalendarItemCommand command);

    void updateCalendarItem(CalendarItemId calendarItemId, UpdateCalendarItemCommand command);

    void deleteCalendarItem(CalendarItemId calendarItemId);
}
