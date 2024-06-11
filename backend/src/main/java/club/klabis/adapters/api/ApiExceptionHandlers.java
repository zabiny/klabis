package club.klabis.adapters.api;

import club.klabis.domain.members.MemberNotFoundException;
import club.klabis.domain.members.MemberRegistrationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ApiExceptionHandlers {

    @ExceptionHandler(MemberRegistrationException.class)
    public ErrorResponse handleMeberRegistrationError(MemberRegistrationException error) {
        return ErrorResponse.builder(error, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage())).build();
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ErrorResponse handleMeberRegistrationError(MemberNotFoundException error) {
        return ErrorResponse.builder(error, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage())).build();
    }

}
