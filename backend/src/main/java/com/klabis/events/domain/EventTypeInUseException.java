package com.klabis.events.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.events.EventTypeId;

import java.util.List;
import java.util.Map;

public class EventTypeInUseException extends ConflictException implements UserFacingError {

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

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("eventNames", affectedEventNames);
    }
}
