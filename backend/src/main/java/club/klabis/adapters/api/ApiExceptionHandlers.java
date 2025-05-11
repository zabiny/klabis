package club.klabis.adapters.api;

import club.klabis.domain.members.IncorrectFormDataException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
class ApiExceptionHandlers extends ResponseEntityExceptionHandler{

    @ExceptionHandler(IncorrectFormDataException.class)
    public ErrorResponse handleMeberRegistrationError(IncorrectFormDataException error) {
        return ErrorResponse.builder(error, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage())).build();
    }

}
