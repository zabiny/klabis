package com.klabis.events.application;

import com.klabis.events.domain.Event;
import com.klabis.events.EventId;
import com.klabis.events.domain.EventStatus;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@PrimaryPort
public interface EventManagementService {

    Event createEvent(Event.EventCommand command);

    void updateEvent(EventId eventId, Event.EventCommand command);

    void publishEvent(EventId eventId);

    void cancelEvent(EventId eventId);

    void finishEvent(EventId eventId);

    Event getEvent(EventId eventId);

    Page<Event> listEvents(Pageable pageable);

    Page<Event> listEventsExcludingStatus(EventStatus excludedStatus, Pageable pageable);

    Page<Event> listEventsByStatus(EventStatus status, Pageable pageable);
}
