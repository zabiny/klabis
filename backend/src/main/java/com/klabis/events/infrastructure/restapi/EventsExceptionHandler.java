package com.klabis.events.infrastructure.restapi;

import com.klabis.events.application.DuplicateOrisImportException;
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

    @ExceptionHandler(DuplicateOrisImportException.class)
    public ErrorResponse handleDuplicateOrisImportException(DuplicateOrisImportException ex) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, ex.getMessage()).title("Duplicate ORIS Import").build();
    }

    @ExceptionHandler(DuplicateRegistrationException.class)
    public ErrorResponse handleDuplicateRegistrationException(DuplicateRegistrationException ex) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, "").title("Registration Conflict").build();
    }

    /**
     * design.md D18: the single-event sync endpoint refuses with a problem detail
     * pointing at the sync resource when the record needs a decision (task 8.3). The
     * pointer itself is the message text — {@link EventSyncNeedsResolutionException}
     * names the event; the client is expected to already know
     * {@code GET /api/events/{id}/sync} from the {@code sync} link on the event
     * resource (task 8.6).
     */
    @ExceptionHandler(EventSyncNeedsResolutionException.class)
    public ErrorResponse handleEventSyncNeedsResolution(EventSyncNeedsResolutionException ex) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, ex.getMessage()).title("Synchronisation Needs A Decision").build();
    }
}
