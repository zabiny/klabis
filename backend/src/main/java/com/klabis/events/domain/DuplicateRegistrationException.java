package com.klabis.events.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.events.EventId;
import com.klabis.members.MemberId;

import java.util.Map;

/**
 * Exception thrown when a member attempts to register for an event they are already registered for.
 */
public class DuplicateRegistrationException extends ConflictException implements UserFacingError {

    private final String eventName;

    public DuplicateRegistrationException(MemberId memberId, EventId eventId, String eventName) {
        super("Member " + memberId.uuid() + " is already registered for event " + eventId.value());
        this.eventName = eventName;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("eventName", eventName);
    }
}
