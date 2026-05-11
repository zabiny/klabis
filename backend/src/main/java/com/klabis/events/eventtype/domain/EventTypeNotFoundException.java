package com.klabis.events.eventtype.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventTypeId;

public class EventTypeNotFoundException extends BusinessRuleViolationException {

    public EventTypeNotFoundException(EventTypeId id) {
        super("Event type not found: " + id);
    }
}
