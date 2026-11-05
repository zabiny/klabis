package com.klabis.events.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.events.EventId;

import java.util.Map;
import java.util.Set;

/**
 * Exception thrown when an event cannot be found by ID.
 * <p>
 * Maps to HTTP 404 Not Found status.
 */
public class EventNotFoundException extends ResourceNotFoundException implements UserFacingError {

    private static final String BY_ID = "event-not-found";
    private static final String BY_ORIS_ID = "oris-event-not-found";

    private final Integer orisId;


    public EventNotFoundException(EventId eventId) {
        super("Event not found with ID: " + eventId);
        this.orisId = null;
    }

    public EventNotFoundException(int orisId) {
        super("ORIS event not found with ID: " + orisId);
        this.orisId = orisId;
    }

    @Override
    public String errorType() {
        return orisId == null ? BY_ID : BY_ORIS_ID;
    }

    @Override
    public Set<String> errorTypes() {
        return Set.of(BY_ID, BY_ORIS_ID);
    }

    @Override
    public Map<String, Object> messageParameters() {
        return orisId == null ? Map.of() : Map.of("orisId", orisId);
    }
}
