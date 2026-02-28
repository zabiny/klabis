package com.klabis.events.registration;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.events.EventId;

/**
 * Exception thrown when an event is not found.
 */
class EventNotFoundException extends ResourceNotFoundException {

    public EventNotFoundException(EventId eventId) {
        super("Event not found with ID: " + eventId);
    }
}
