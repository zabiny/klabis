package club.klabis.adapters.api;

import club.klabis.domain.appusers.ApplicationUserNotFound;
import club.klabis.domain.members.MemberNotFoundException;
import club.klabis.domain.members.IncorrectRegistrationDataException;
import club.klabis.domain.members.MemberRegistrationFailedException;
import club.klabis.domain.members.MembershipCannotBeSuspendedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ApiExceptionHandlers {

    @ExceptionHandler(MemberRegistrationFailedException.class)
    public ErrorResponse handleMeberRegistrationError(MemberRegistrationFailedException error) {
        return ErrorResponse.builder(error, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage())).build();
    }

    @ExceptionHandler(IncorrectRegistrationDataException.class)
    public ErrorResponse handleMeberRegistrationError(IncorrectRegistrationDataException error) {
        return ErrorResponse.builder(error, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage())).build();
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ErrorResponse handleMeberRegistrationError(MemberNotFoundException error) {
        return ErrorResponse.builder(error, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage())).build();
    }

    @ExceptionHandler(MembershipCannotBeSuspendedException.class)
    public ErrorResponse handleMembershipSuspensionFailed(MembershipCannotBeSuspendedException error) {
        return ErrorResponse.builder(error, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage())).build();
    }

    @ExceptionHandler(ApplicationUserNotFound.class)
    public ErrorResponse handleMembershipSuspensionFailed(ApplicationUserNotFound error) {
        return ErrorResponse.builder(error, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage())).build();
    }

}
