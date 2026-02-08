package com.klabis.events.registration;

import com.klabis.common.exceptions.ResourceNotFoundException;

import java.util.UUID;

/**
 * Exception thrown when an event is not found.
 */
class EventNotFoundException extends ResourceNotFoundException {

    public EventNotFoundException(UUID eventId) {
        super("Event not found with ID: " + eventId);
    }
}
