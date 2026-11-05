package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Set;

public class EventNotUpdatableException extends BusinessRuleViolationException implements UserFacingError {

    private static final String FINISHED = "finished-event-cannot-be-updated";
    private static final String CANCELLED = "cancelled-event-cannot-be-updated";

    private final String errorType;

    private EventNotUpdatableException(String message, String errorType) {
        super(message);
        this.errorType = errorType;
    }

    public static EventNotUpdatableException finished() {
        return new EventNotUpdatableException("Cannot update event in FINISHED status", FINISHED);
    }

    public static EventNotUpdatableException cancelled() {
        return new EventNotUpdatableException("Cannot update event in CANCELLED status", CANCELLED);
    }

    @Override
    public String errorType() {
        return errorType;
    }

    @Override
    public Set<String> errorTypes() {
        return Set.of(FINISHED, CANCELLED);
    }
}
