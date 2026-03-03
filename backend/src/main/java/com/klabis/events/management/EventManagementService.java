package com.klabis.events.management;

import com.klabis.events.Event;
import com.klabis.events.EventId;
import com.klabis.events.EventStatus;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@PrimaryPort
public interface EventManagementService {

    UUID createEvent(Event.CreateCommand command);

    void updateEvent(EventId eventId, Event.UpdateCommand command);

    void publishEvent(EventId eventId);

    void cancelEvent(EventId eventId);

    void finishEvent(EventId eventId);

    Event getEvent(EventId eventId);

    Page<Event> listEvents(Pageable pageable);

    Page<Event> listEventsByStatus(EventStatus status, Pageable pageable);
}
