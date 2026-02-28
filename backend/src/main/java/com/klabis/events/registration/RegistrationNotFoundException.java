package com.klabis.events.registration;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.events.EventId;
import com.klabis.members.MemberId;

/**
 * Exception thrown when a registration is not found.
 */
class RegistrationNotFoundException extends ResourceNotFoundException {

    public RegistrationNotFoundException(MemberId memberId, EventId eventId) {
        super("Member " + memberId.uuid() + " is not registered for event " + eventId.value());
    }
}
