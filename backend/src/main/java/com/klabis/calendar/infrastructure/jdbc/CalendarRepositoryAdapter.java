package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.calendar.CalendarItemKind;
import com.klabis.calendar.domain.CalendarFilter;
import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.domain.CalendarRepository;
import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.domain.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public List<CalendarItem> findByFilter(CalendarFilter filter, Sort sort) {
        boolean hasKinds = !filter.itemTypes().isEmpty();
        boolean hasEventIds = !filter.eventIds().isEmpty();

        List<CalendarMemento> mementos;
        if (hasKinds && hasEventIds) {
            mementos = jdbcRepository.findByDateRangeAndKindsAndEventIds(
                    filter.startDate(), filter.endDate(),
                    toKindStrings(filter.itemTypes()),
                    toUuids(filter.eventIds()));
        } else if (hasKinds) {
            mementos = jdbcRepository.findByDateRangeAndKinds(
                    filter.startDate(), filter.endDate(),
                    toKindStrings(filter.itemTypes()));
        } else if (hasEventIds) {
            mementos = jdbcRepository.findByDateRangeAndEventIds(
                    filter.startDate(), filter.endDate(),
                    toUuids(filter.eventIds()));
        } else {
            mementos = jdbcRepository.findByDateRange(filter.startDate(), filter.endDate());
        }

        return mementos.stream()
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

    private static Collection<String> toKindStrings(Set<CalendarItemKind> kinds) {
        return kinds.stream().map(CalendarItemKind::name).collect(Collectors.toSet());
    }

    private static Collection<UUID> toUuids(Set<EventId> eventIds) {
        return eventIds.stream().map(EventId::value).collect(Collectors.toSet());
    }
}
