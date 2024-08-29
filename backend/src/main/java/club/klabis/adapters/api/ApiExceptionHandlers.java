package club.klabis.adapters.api;

import club.klabis.domain.appusers.ApplicationUserNotFound;
import club.klabis.domain.members.MemberNotFoundException;
import club.klabis.domain.members.IncorrectFormDataException;
import club.klabis.domain.members.MemberRegistrationFailedException;
import club.klabis.domain.members.MembershipCannotBeSuspendedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
class ApiExceptionHandlers {

    @ExceptionHandler(IncorrectFormDataException.class)
    public ErrorResponse handleMeberRegistrationError(IncorrectFormDataException error) {
        return ErrorResponse.builder(error, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage())).build();
    }

}
