package com.klabis.calendar.application;

import com.klabis.calendar.domain.CalendarItem;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@PrimaryPort
public interface CalendarManagementPort {

    List<CalendarItem> listCalendarItems(@NonNull LocalDate startDate, @NonNull LocalDate endDate, Sort sort);

    CalendarItem getCalendarItem(UUID calendarItemId);

    UUID createCalendarItem(CreateCalendarItemCommand command);

    void updateCalendarItem(UUID calendarItemId, UpdateCalendarItemCommand command);

    void deleteCalendarItem(UUID calendarItemId);
}
