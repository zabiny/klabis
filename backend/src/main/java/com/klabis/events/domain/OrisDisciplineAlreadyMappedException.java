package com.klabis.events.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

public class OrisDisciplineAlreadyMappedException extends ConflictException implements UserFacingError {

    private final String eventTypeName;

    public OrisDisciplineAlreadyMappedException(String eventTypeName) {
        super("A discipline is already mapped to event type '" + eventTypeName + "'");
        this.eventTypeName = eventTypeName;
    }

    /**
     * Used when a unique-constraint violation is caught at the persistence boundary
     * (TOCTOU race between the application-layer check and the save) without the
     * conflicting event type at hand.
     */
    public OrisDisciplineAlreadyMappedException() {
        super("A discipline in this event type is already mapped to another event type");
        this.eventTypeName = null;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return eventTypeName == null ? Map.of() : Map.of("eventTypeName", eventTypeName);
    }
}
