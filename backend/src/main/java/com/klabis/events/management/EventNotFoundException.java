package com.klabis.events.management;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.events.EventId;

/**
 * Exception thrown when an event cannot be found by ID.
 * <p>
 * Maps to HTTP 404 Not Found status.
 */
class EventNotFoundException extends ResourceNotFoundException {

    /**
     * Creates a new EventNotFoundException.
     *
     * @param eventId the ID of the event that was not found
     */
    public EventNotFoundException(EventId eventId) {
        super("Event not found with ID: " + eventId);
    }
}
