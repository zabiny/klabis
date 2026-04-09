package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

@PrimaryPort
public interface EventManagementPort {

    Event createEvent(Event.CreateEvent command);

    Event importEventFromOris(int orisId);

    void updateEvent(EventId eventId, Event.UpdateEvent command);

    void publishEvent(EventId eventId);

    void cancelEvent(EventId eventId);

    void finishExpiredActiveEvents(LocalDate currentDate);

    void syncEventFromOris(EventId eventId);

    Event getEvent(EventId eventId, boolean canManageEvents);

    Page<Event> listEvents(EventFilter filter, Pageable pageable, boolean canManageEvents);
}
