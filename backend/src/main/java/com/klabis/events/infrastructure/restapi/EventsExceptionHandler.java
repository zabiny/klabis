package com.klabis.events.infrastructure.restapi;

import com.klabis.events.application.EventSyncNeedsResolutionException;
import com.klabis.events.domain.DuplicateRegistrationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = EventController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class EventsExceptionHandler {

    @ExceptionHandler(DuplicateRegistrationException.class)
    public ErrorResponse handleDuplicateRegistrationException(DuplicateRegistrationException ex) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, "").title("Registration Conflict").build();
    }

    /**
     * design.md D18: importing an ORIS event whose pairing already awaits a decision
     * refuses with a problem detail pointing at the sync resource (task 9.3). The
     * pointer itself is the message text — {@link EventSyncNeedsResolutionException}
     * names the ORIS id; the client is expected to already know
     * {@code GET /api/events/{id}/sync} from the {@code sync} link on the event
     * resource (task 8.6).
     */
    @ExceptionHandler(EventSyncNeedsResolutionException.class)
    public ErrorResponse handleEventSyncNeedsResolution(EventSyncNeedsResolutionException ex) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, ex.getMessage()).title("Synchronisation Needs A Decision").build();
    }
}
