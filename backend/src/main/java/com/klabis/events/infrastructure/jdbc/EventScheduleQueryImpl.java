package com.klabis.events.infrastructure.jdbc;

import com.klabis.events.EventId;
import com.klabis.events.EventScheduleQuery;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.Events;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@SecondaryAdapter
class EventScheduleQueryImpl implements EventScheduleQuery {

    private final Events events;

    EventScheduleQueryImpl(Events events) {
        this.events = events;
    }

    @Override
    public Set<EventId> findEventIdsByRegistration(MemberId memberId, LocalDate from, LocalDate to) {
        EventFilter filter = EventFilter.none()
                .withDateRange(from, to)
                .withRegisteredBy(memberId);
        return collectEventIds(filter);
    }

    @Override
    public Set<EventId> findEventIdsByCoordinator(MemberId memberId, LocalDate from, LocalDate to) {
        EventFilter filter = EventFilter.none()
                .withDateRange(from, to)
                .withCoordinator(memberId);
        return collectEventIds(filter);
    }

    private Set<EventId> collectEventIds(EventFilter filter) {
        Page<com.klabis.events.domain.Event> page = events.findAll(filter, Pageable.unpaged());
        return page.stream()
                .map(com.klabis.events.domain.Event::getId)
                .collect(Collectors.toSet());
    }
}
