package com.klabis.events.application;

import com.klabis.common.exceptions.AuthorizationException;

public class AccommodationListAccessDeniedException extends AuthorizationException {

    private AccommodationListAccessDeniedException(String message) {
        super(message);
    }

    public static AccommodationListAccessDeniedException callerNotPermitted() {
        return new AccommodationListAccessDeniedException(
                "Access to accommodation list requires EVENTS:REGISTRATIONS authority or being the event coordinator");
    }

    public static AccommodationListAccessDeniedException sharedAccommodationNotOffered() {
        return new AccommodationListAccessDeniedException(
                "Accommodation list is available only when the event offers shared accommodation");
    }
}
