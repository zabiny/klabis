package com.klabis.events.eventtype.application;

import com.klabis.events.EventTypeId;
import com.klabis.events.eventtype.domain.EventType;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface EventTypeManagementPort {

    EventType createEventType(EventType.CreateEventType command);

    void updateEventType(EventTypeId id, EventType.UpdateEventType command);

    void deleteEventType(EventTypeId id);

    EventType getEventType(EventTypeId id);

    List<EventType> listAllSorted();

    /**
     * Returns ORIS discipline IDs available for assignment to event types.
     * Returns an empty list when ORIS integration is not active.
     */
    List<String> listDisciplineOptions();
}
