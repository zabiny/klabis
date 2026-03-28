package com.klabis.events.infrastructure.restapi;

import com.klabis.events.application.DuplicateOrisImportException;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = EventController.class)
class EventsExceptionHandler {

    @ExceptionHandler(DuplicateOrisImportException.class)
    public ErrorResponse handleDuplicateOrisImportException(DuplicateOrisImportException ex) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, ex.getMessage()).title("Duplicate ORIS Import").build();
    }
}
