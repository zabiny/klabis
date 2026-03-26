package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventManagementServiceImpl implements EventManagementService {

    private final EventRepository eventRepository;

    EventManagementServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    @Override
    public Event createEvent(Event.EventCommand command) {
        Event event = Event.create(
                command.name(),
                command.eventDate(),
                command.location(),
                command.organizer(),
                command.websiteUrl() != null ? WebsiteUrl.of(command.websiteUrl()) : null,
                command.eventCoordinatorId()
        );

        return eventRepository.save(event);
    }

    @Transactional
    @Override
    public void updateEvent(EventId eventId, Event.EventCommand command) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        event.update(
                command.name(),
                command.eventDate(),
                command.location(),
                command.organizer(),
                command.websiteUrl() != null ? WebsiteUrl.of(command.websiteUrl()) : null,
                command.eventCoordinatorId()
        );

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
    public void finishEvent(EventId eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        event.finish();
        eventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Event getEvent(EventId eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Event> listEvents(EventFilter filter, Pageable pageable) {
        return eventRepository.findAll(filter, pageable);
    }
}
