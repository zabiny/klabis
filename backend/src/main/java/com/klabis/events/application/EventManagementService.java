package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.*;
import com.klabis.oris.apiclient.OrisApiClient;
import com.klabis.oris.apiclient.dto.EventDetails;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class EventManagementService implements EventManagementPort {

    private final EventRepository eventRepository;
    private final Optional<OrisApiClient> orisApiClient;

    EventManagementService(EventRepository eventRepository, Optional<OrisApiClient> orisApiClient) {
        this.eventRepository = eventRepository;
        this.orisApiClient = orisApiClient;
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
    public void finishEvent(EventId eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        event.finish();
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
        LocalDate registrationDeadline = details.entryDate1() != null ? details.entryDate1().toLocalDate() : null;

        Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(orisId)
                .name(details.name())
                .eventDate(details.date())
                .location(details.place())
                .organizer(organizer)
                .websiteUrl(websiteUrl)
                .registrationDeadline(registrationDeadline)
                .build());

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
