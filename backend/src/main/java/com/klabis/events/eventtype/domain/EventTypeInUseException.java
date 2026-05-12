package com.klabis.events.eventtype.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventTypeId;

import java.util.List;

public class EventTypeInUseException extends BusinessRuleViolationException {

    private final EventTypeId eventTypeId;
    private final List<String> affectedEventNames;

    public EventTypeInUseException(EventTypeId eventTypeId, List<String> affectedEventNames) {
        super("Event type is still used by: " + String.join(", ", affectedEventNames));
        this.eventTypeId = eventTypeId;
        this.affectedEventNames = List.copyOf(affectedEventNames);
    }

    public EventTypeId getEventTypeId() {
        return eventTypeId;
    }

    public List<String> getAffectedEventNames() {
        return affectedEventNames;
    }
}
