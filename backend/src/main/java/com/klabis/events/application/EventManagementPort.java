package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

@PrimaryPort
public interface EventManagementPort {

    Event createEvent(Event.CreateEvent command);

    void updateEvent(EventId eventId, Event.UpdateEvent command);

    void publishEvent(EventId eventId);

    void cancelEvent(EventId eventId, Event.CancelEvent command);

    void finishExpiredActiveEvents(LocalDate currentDate);

    Event getEvent(EventId eventId, boolean canManageEvents);

    /**
     * @param viewerMemberId the calling member, {@code null} when the user has no member profile;
     *                       ignored when {@code canManageEvents} is true
     */
    Page<Event> listEvents(EventFilter filter, Pageable pageable, boolean canManageEvents, MemberId viewerMemberId);
}
