package com.klabis.calendar.application;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@PrimaryPort
public interface CalendarManagementPort {

    List<CalendarItemDto> listCalendarItems(@NonNull LocalDate startDate, @NonNull LocalDate endDate, Sort sort);

    CalendarItemDto getCalendarItem(UUID calendarItemId);

    UUID createCalendarItem(CreateCalendarItemCommand command);

    void updateCalendarItem(UUID calendarItemId, UpdateCalendarItemCommand command);

    void deleteCalendarItem(UUID calendarItemId);
}
