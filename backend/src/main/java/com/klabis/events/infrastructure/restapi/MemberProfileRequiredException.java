package com.klabis.events.infrastructure.restapi;

import com.klabis.common.exceptions.AuthorizationException;

/**
 * Exception thrown when a user attempts to register for an event
 * but does not have an associated member profile.
 * <p>
 * This enforces the business rule that only users with member profiles
 * can register for club events.
 */
public class MemberProfileRequiredException extends AuthorizationException {

    public MemberProfileRequiredException() {
        super("User must have a member profile to register for events");
    }

    public MemberProfileRequiredException(String message) {
        super(message);
    }
}
