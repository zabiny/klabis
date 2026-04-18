package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class EventManagementService implements EventManagementPort {

    private final EventRepository eventRepository;

    EventManagementService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    @Override
    public Event createEvent(Event.CreateEvent command) {
        Event event = Event.create(command);
        return eventRepository.save(event);
    }

    @Transactional
    @Override
    public void updateEvent(EventId eventId, Event.UpdateEvent command) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        event.update(command);

        eventRepository.save(event);
    }

    @Transactional
    @Override
    public void publishEvent(EventId eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        event.publish();
        eventRepository.save(event);
    }

    @Transactional
    @Override
    public void cancelEvent(EventId eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        event.cancel();
        eventRepository.save(event);
    }

    @Transactional
    @Override
    public void finishExpiredActiveEvents(LocalDate currentDate) {
        eventRepository.findAll(EventFilter.activeEventsWithDateBefore(currentDate), Pageable.unpaged()).getContent().forEach(event -> {
            event.finish();
            eventRepository.save(event);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Event getEvent(EventId eventId, boolean canManageEvents) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (!canManageEvents && event.getStatus() == EventStatus.DRAFT) {
            throw new EventNotFoundException(eventId);
        }
        return event;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Event> listEvents(EventFilter filter, Pageable pageable, boolean canManageEvents) {
        if (canManageEvents || filter.excludesStatus(EventStatus.DRAFT)) {
            return eventRepository.findAll(filter, pageable);
        }
        if (filter.requestsOnlyStatus(EventStatus.DRAFT)) {
            return Page.empty(pageable);
        }
        return eventRepository.findAll(filter.withExcludedStatus(EventStatus.DRAFT), pageable);
    }
}
