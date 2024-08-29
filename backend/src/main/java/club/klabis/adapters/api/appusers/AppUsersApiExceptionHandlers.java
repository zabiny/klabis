package club.klabis.adapters.api.appusers;

import club.klabis.domain.appusers.ApplicationUserNotFound;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(basePackageClasses = AppUsersApiExceptionHandlers.class)
class AppUsersApiExceptionHandlers {

    @ExceptionHandler(ApplicationUserNotFound.class)
    public ErrorResponse handleMembershipSuspensionFailed(ApplicationUserNotFound error) {
        return ErrorResponse.builder(error, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage())).build();
    }

}
