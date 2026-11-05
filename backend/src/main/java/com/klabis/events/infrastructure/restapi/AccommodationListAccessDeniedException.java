package com.klabis.events.infrastructure.restapi;

import com.klabis.common.exceptions.AuthorizationException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Set;

class AccommodationListAccessDeniedException extends AuthorizationException implements UserFacingError {

    private static final String CALLER_NOT_PERMITTED = "accommodation-list-access-denied";
    private static final String SHARED_ACCOMMODATION_NOT_OFFERED = "shared-accommodation-not-offered";

    private final String errorType;

    private AccommodationListAccessDeniedException(String message, String errorType) {
        super(message);
        this.errorType = errorType;
    }

    static AccommodationListAccessDeniedException callerNotPermitted() {
        return new AccommodationListAccessDeniedException(
                "Access to accommodation list requires EVENTS:REGISTRATIONS authority or being the event coordinator",
                CALLER_NOT_PERMITTED);
    }

    static AccommodationListAccessDeniedException sharedAccommodationNotOffered() {
        return new AccommodationListAccessDeniedException(
                "Accommodation list is available only when the event offers shared accommodation",
                SHARED_ACCOMMODATION_NOT_OFFERED);
    }

    @Override
    public String errorType() {
        return errorType;
    }

    @Override
    public Set<String> errorTypes() {
        return Set.of(CALLER_NOT_PERMITTED, SHARED_ACCOMMODATION_NOT_OFFERED);
    }
}
