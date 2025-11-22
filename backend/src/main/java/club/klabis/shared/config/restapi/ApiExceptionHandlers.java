package club.klabis.shared.config.restapi;

import club.klabis.shared.domain.IncorrectFormDataException;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

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

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                status,
                "Validation failed for one or more fields"
        );

        problemDetail.setTitle("Validation Error");
        //problemDetail.setType(URI.create("https://api.example.com/errors/validation"));

        // Přidání detailů validačních chyb
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = error instanceof FieldError fieldError ? fieldError.getField() : "*";
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        problemDetail.setProperty("errors", errors);

        return ResponseEntity.status(status).body(problemDetail);
    }


    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ErrorResponse handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Constraint violation occurred"
        );

        problemDetail.setTitle("Validation Error");
        //problemDetail.setType(URI.create("https://api.example.com/errors/validation"));

        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        });

        problemDetail.setProperty("violations", errors);

        return ErrorResponse.builder(ex, problemDetail).build();
    }

}
