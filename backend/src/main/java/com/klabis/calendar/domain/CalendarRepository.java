package com.klabis.calendar.domain;

import com.klabis.calendar.CalendarItemId;
import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@SecondaryPort
public interface CalendarRepository {

    CalendarItem save(CalendarItem calendarItem);

    Optional<CalendarItem> findById(CalendarItemId id);

    List<CalendarItem> findByDateRange(LocalDate startDate, LocalDate endDate);

    Optional<CalendarItem> findByEventId(EventId eventId);

    void delete(CalendarItem calendarItem);
}
