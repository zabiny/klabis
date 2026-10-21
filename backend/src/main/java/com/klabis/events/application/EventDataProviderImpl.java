package com.klabis.events.application;

import com.klabis.events.EventData;
import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventStatus;
import org.jmolecules.ddd.annotation.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
class EventDataProviderImpl implements EventDataProvider {

    private final EventRepository eventRepository;

    EventDataProviderImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public EventData getEventData(EventId eventId) {
        return eventRepository.findById(eventId)
                .map(EventDataProviderImpl::toEventData)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
    }

    @Override
    public List<EventData> getEventData(Collection<EventId> eventIds) {
        return eventIds.stream()
                .map(eventRepository::findById)
                .flatMap(Optional::stream)
                .map(EventDataProviderImpl::toEventData)
                .toList();
    }

    private static EventData toEventData(Event event) {
        return new EventData(
                event.getId(),
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getOrganizer(),
                event.getWebsiteUrl() != null ? event.getWebsiteUrl().value() : null,
                event.getRegistrationDeadlines().deadline1().orElse(null),
                event.getStatus() == EventStatus.CANCELLED,
                event.getCoordinators()
        );
    }
}
