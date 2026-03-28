package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.EventRepository;
import com.klabis.oris.apiclient.OrisApiClient;
import com.klabis.oris.apiclient.dto.EventDetails;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EventManagementServiceImpl implements EventManagementService {

    private final EventRepository eventRepository;
    private final Optional<OrisApiClient> orisApiClient;

    EventManagementServiceImpl(EventRepository eventRepository, Optional<OrisApiClient> orisApiClient) {
        this.eventRepository = eventRepository;
        this.orisApiClient = orisApiClient;
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

    private static final String UNKNOWN_ORGANIZER = "---";

    @Transactional
    @Override
    public Event importEventFromOris(int orisId) {
        OrisApiClient client = orisApiClient.orElseThrow(() ->
                new IllegalStateException("ORIS integration is not active"));

        EventDetails details = client.getEventDetails(orisId).payload()
                .orElseThrow(() -> new EventNotFoundException(orisId));

        String organizer = resolveOrganizer(details);
        WebsiteUrl websiteUrl = WebsiteUrl.of(client.getEventWebUrl(orisId));

        Event event = Event.createFromOris(
                orisId,
                details.name(),
                details.date(),
                details.place(),
                organizer,
                websiteUrl
        );

        try {
            return eventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateOrisImportException(orisId);
        }
    }

    private String resolveOrganizer(EventDetails details) {
        if (details.org1() != null && details.org1().abbreviation() != null && !details.org1().abbreviation().isBlank()) {
            return details.org1().abbreviation();
        }
        if (details.org2() != null && details.org2().abbreviation() != null && !details.org2().abbreviation().isBlank()) {
            return details.org2().abbreviation();
        }
        return UNKNOWN_ORGANIZER;
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
