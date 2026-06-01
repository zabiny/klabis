package com.klabis.events.eventtype.infrastructure.restapi;

import com.klabis.events.eventtype.domain.EventTypeInUseException;
import com.klabis.events.eventtype.domain.EventTypeNameAlreadyExistsException;
import com.klabis.events.eventtype.domain.EventTypeNotFoundException;
import com.klabis.events.eventtype.domain.OrisDisciplineAlreadyMappedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = EventTypeController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class EventTypeExceptionHandler {

    @ExceptionHandler(EventTypeNotFoundException.class)
    ErrorResponse handleNotFound(EventTypeNotFoundException ex) {
        return ErrorResponse.builder(ex, HttpStatus.NOT_FOUND, ex.getMessage())
                .title("Event Type Not Found")
                .build();
    }

    @ExceptionHandler(EventTypeInUseException.class)
    ErrorResponse handleInUse(EventTypeInUseException ex) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, ex.getMessage())
                .title("Event Type In Use")
                .build();
    }

    @ExceptionHandler(EventTypeNameAlreadyExistsException.class)
    ErrorResponse handleNameConflict(EventTypeNameAlreadyExistsException ex) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, ex.getMessage())
                .title("Event Type Name Already Exists")
                .build();
    }

    @ExceptionHandler(OrisDisciplineAlreadyMappedException.class)
    ErrorResponse handleOrisDisciplineConflict(OrisDisciplineAlreadyMappedException ex) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, ex.getMessage())
                .title("ORIS Discipline Already Mapped")
                .build();
    }
}
