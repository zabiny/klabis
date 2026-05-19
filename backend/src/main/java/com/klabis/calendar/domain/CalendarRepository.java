package com.klabis.calendar.domain;

import com.klabis.calendar.CalendarItemId;
import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

@Repository
@SecondaryPort
public interface CalendarRepository {

    CalendarItem save(CalendarItem calendarItem);

    Optional<CalendarItem> findById(CalendarItemId id);

    List<CalendarItem> findByFilter(CalendarFilter filter, Sort sort);

    List<CalendarItem> findByEventId(EventId eventId);

    void delete(CalendarItem calendarItem);
}
