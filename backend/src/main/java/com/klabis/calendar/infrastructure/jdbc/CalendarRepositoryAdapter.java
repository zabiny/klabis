package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.domain.CalendarRepository;
import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class CalendarRepositoryAdapter implements CalendarRepository {

    private final CalendarJdbcRepository jdbcRepository;

    public CalendarRepositoryAdapter(CalendarJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public CalendarItem save(CalendarItem calendarItem) {
        CalendarMemento saved = jdbcRepository.save(CalendarMemento.from(calendarItem));
        return saved.toCalendarItem();
    }

    @Override
    public Optional<CalendarItem> findById(CalendarItemId id) {
        return jdbcRepository.findById(id.value())
                .map(CalendarMemento::toCalendarItem);
    }

    @Override
    public List<CalendarItem> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return jdbcRepository.findByDateRange(startDate, endDate).stream()
                .map(CalendarMemento::toCalendarItem)
                .toList();
    }

    @Override
    public List<CalendarItem> findByEventId(EventId eventId) {
        return jdbcRepository.findByEventId(eventId.value()).stream()
                .map(CalendarMemento::toCalendarItem)
                .toList();
    }

    @Override
    public void delete(CalendarItem calendarItem) {
        jdbcRepository.deleteById(calendarItem.getId().value());
    }
}
