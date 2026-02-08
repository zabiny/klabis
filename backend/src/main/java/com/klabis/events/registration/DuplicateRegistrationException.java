package com.klabis.events.registration;

import com.klabis.common.exceptions.BusinessRuleViolationException;

import java.util.UUID;

/**
 * Exception thrown when a member attempts to register for an event they are already registered for.
 */
class DuplicateRegistrationException extends BusinessRuleViolationException {

    public DuplicateRegistrationException(UUID memberId, UUID eventId) {
        super("Member " + memberId + " is already registered for event " + eventId);
    }
}
