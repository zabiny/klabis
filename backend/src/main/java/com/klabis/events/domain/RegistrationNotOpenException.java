package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Set;

public class RegistrationNotOpenException extends BusinessRuleViolationException implements UserFacingError {

    private static final String EVENT_NOT_ACTIVE = "event-not-active-for-registration";
    private static final String DEADLINE_PASSED = "registration-deadline-passed";
    private static final String EVENT_STARTED = "registration-closed-after-event-date";

    private final String errorType;

    private RegistrationNotOpenException(String message, String errorType) {
        super(message);
        this.errorType = errorType;
    }

    public static RegistrationNotOpenException eventNotActive() {
        return new RegistrationNotOpenException("Registration is only allowed for ACTIVE events", EVENT_NOT_ACTIVE);
    }

    public static RegistrationNotOpenException deadlinePassed() {
        return new RegistrationNotOpenException("Registration deadline has passed", DEADLINE_PASSED);
    }

    public static RegistrationNotOpenException eventStarted() {
        return new RegistrationNotOpenException("Cannot register/unregister on or after event date", EVENT_STARTED);
    }

    @Override
    public String errorType() {
        return errorType;
    }

    @Override
    public Set<String> errorTypes() {
        return Set.of(EVENT_NOT_ACTIVE, DEADLINE_PASSED, EVENT_STARTED);
    }
}
