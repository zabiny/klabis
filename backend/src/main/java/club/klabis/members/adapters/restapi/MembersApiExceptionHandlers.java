package club.klabis.members.adapters.restapi;

import club.klabis.members.domain.MemberNotFoundException;
import club.klabis.members.domain.MemberRegistrationFailedException;
import club.klabis.members.domain.MembershipCannotBeSuspendedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(basePackageClasses = MembersApiExceptionHandlers.class)
class MembersApiExceptionHandlers {

    @ExceptionHandler(MemberRegistrationFailedException.class)
    public ErrorResponse handleMeberRegistrationError(MemberRegistrationFailedException error) {
        return ErrorResponse.builder(error, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage())).build();
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ErrorResponse handleMeberRegistrationError(MemberNotFoundException error) {
        return ErrorResponse.builder(error, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage())).build();
    }

    @ExceptionHandler(MembershipCannotBeSuspendedException.class)
    public ErrorResponse handleMembershipSuspensionFailed(MembershipCannotBeSuspendedException error) {
        return ErrorResponse.builder(error, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage())).build();
    }

}
