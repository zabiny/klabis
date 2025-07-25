package club.klabis.shared.config.restapi;

import club.klabis.shared.domain.IncorrectFormDataException;
import org.springframework.http.*;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
class ApiExceptionHandlers extends ResponseEntityExceptionHandler{

    @ExceptionHandler(IncorrectFormDataException.class)
    public ErrorResponse handleMeberRegistrationError(IncorrectFormDataException error) {
        return ErrorResponse.builder(error, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage())).build();
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        logger.warn("Handled API exception: %s".formatted(ex.getMessage()));
        if (logger.isDebugEnabled()) {
            logger.debug("Full exception details:", ex);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}
