package com.klabis.events.eventtype.application;

import com.klabis.common.ui.HalFormsInlineOption;
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
     * Returns ORIS discipline options available for assignment to event types.
     * Each option carries a machine value (discipline ID string) and a human-readable Czech prompt.
     * Returns an empty list when ORIS integration is not active.
     */
    List<HalFormsInlineOption> listDisciplineOptions();
}
