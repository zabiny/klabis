package com.klabis.calendar.domain;

import com.klabis.calendar.CalendarItemId;
import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@SecondaryPort
public interface CalendarRepository {

    CalendarItem save(CalendarItem calendarItem);

    Optional<CalendarItem> findById(CalendarItemId id);

    List<CalendarItem> findByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Returns only EVENT_DATE calendar items whose date range overlaps {@code [startDate, endDate]}
     * and whose linked event ID is in {@code eventIds}.
     * When {@code eventIds} is empty, returns an empty list without hitting the database.
     */
    List<CalendarItem> findEventDateItemsByDateRangeAndEventIds(LocalDate startDate, LocalDate endDate, Set<EventId> eventIds);

    List<CalendarItem> findByEventId(EventId eventId);

    void delete(CalendarItem calendarItem);
}
