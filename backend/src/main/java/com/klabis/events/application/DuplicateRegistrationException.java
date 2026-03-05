package com.klabis.events.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventId;
import com.klabis.members.MemberId;

/**
 * Exception thrown when a member attempts to register for an event they are already registered for.
 */
public class DuplicateRegistrationException extends BusinessRuleViolationException {

    public DuplicateRegistrationException(MemberId memberId, EventId eventId) {
        super("Member " + memberId.uuid() + " is already registered for event " + eventId.value());
    }
}
