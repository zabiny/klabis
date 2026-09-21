package com.klabis.events.application;

import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.EventType;
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
     * Returns local discipline options available for assignment to event types.
     * Each option carries a machine value (local discipline ID) and a human-readable prompt (discipline name).
     */
    List<HalFormsInlineOption> listDisciplineOptions();
}
