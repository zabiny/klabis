package com.klabis.events.registration;

import com.klabis.common.exceptions.ResourceNotFoundException;

import java.util.UUID;

/**
 * Exception thrown when a registration is not found.
 */
class RegistrationNotFoundException extends ResourceNotFoundException {

    public RegistrationNotFoundException(UUID memberId, UUID eventId) {
        super("Member " + memberId + " is not registered for event " + eventId);
    }
}
