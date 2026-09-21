package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.DisciplineNotArchivedException;
import com.klabis.events.domain.DisciplineNotEditableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// DisciplineNotFoundException extends ResourceNotFoundException, already mapped to 404 by the
// shared MvcExceptionHandler, so it needs no explicit mapping here.
@RestControllerAdvice(basePackageClasses = DisciplineController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class DisciplineExceptionHandler {

    @ExceptionHandler(DisciplineNotEditableException.class)
    ErrorResponse handleNotEditable(DisciplineNotEditableException ex) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, ex.getMessage())
                .title("Discipline Not Editable")
                .build();
    }

    @ExceptionHandler(DisciplineNotArchivedException.class)
    ErrorResponse handleNotArchived(DisciplineNotArchivedException ex) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, ex.getMessage())
                .title("Discipline Not Archived")
                .build();
    }
}
