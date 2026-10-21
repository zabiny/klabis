package com.klabis.events.domain;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.events.EventTypeId;

public class EventTypeNotFoundException extends ResourceNotFoundException {

    public EventTypeNotFoundException(EventTypeId id) {
        super("Event type not found: " + id);
    }
}
