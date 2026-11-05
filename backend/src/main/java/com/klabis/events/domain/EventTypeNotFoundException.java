package com.klabis.events.domain;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.events.EventTypeId;

public class EventTypeNotFoundException extends ResourceNotFoundException implements UserFacingError {

    public EventTypeNotFoundException(EventTypeId id) {
        super("Event type not found: " + id);
    }
}
