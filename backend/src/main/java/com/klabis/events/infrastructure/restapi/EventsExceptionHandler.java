package com.klabis.events.infrastructure.restapi;

import com.klabis.common.exceptions.CommonErrorTypes;
import com.klabis.events.domain.DuplicateRegistrationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

// Non-standard body: the 409 for a duplicate registration deliberately carries an empty detail.
@RestControllerAdvice(basePackageClasses = EventController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class EventsExceptionHandler {

    @ExceptionHandler(DuplicateRegistrationException.class)
    public ErrorResponse handleDuplicateRegistrationException(DuplicateRegistrationException ex) {
        ErrorResponse.Builder response = ErrorResponse.builder(ex, HttpStatus.CONFLICT, "")
                .title("Registration Conflict")
                .type(URI.create(CommonErrorTypes.uri(ex.errorType())));
        ex.messageParameters().forEach(response::property);
        return response.build();
    }
}
