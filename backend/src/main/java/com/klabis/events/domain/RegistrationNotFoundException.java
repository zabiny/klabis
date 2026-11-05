package com.klabis.events.domain;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.events.EventId;
import com.klabis.members.MemberId;

public class RegistrationNotFoundException extends ResourceNotFoundException implements UserFacingError {

    public RegistrationNotFoundException(MemberId memberId, EventId eventId) {
        super("Member " + memberId.uuid() + " is not registered for event " + eventId.value());
    }
}
