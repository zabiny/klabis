package com.klabis.events.infrastructure.restapi;

import com.klabis.common.exceptions.AuthorizationException;

class AccommodationListAccessDeniedException extends AuthorizationException {

    private AccommodationListAccessDeniedException(String message) {
        super(message);
    }

    static AccommodationListAccessDeniedException callerNotPermitted() {
        return new AccommodationListAccessDeniedException(
                "Access to accommodation list requires EVENTS:REGISTRATIONS authority or being the event coordinator");
    }

    static AccommodationListAccessDeniedException sharedAccommodationNotOffered() {
        return new AccommodationListAccessDeniedException(
                "Accommodation list is available only when the event offers shared accommodation");
    }
}
