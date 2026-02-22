package com.klabis.config.mvc;

import com.klabis.common.exceptions.AuthorizationException;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ApiResponse(
        responseCode = "401",
        description = "Unauthorized - authentication required",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
        )
)
@RestControllerAdvice
class MvcExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessRuleViolationException.class)
    @ApiResponse(
            responseCode = "400",
            description = "Validation error - business rule violated",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleBusinessRuleViolationException(BusinessRuleViolationException ex) {
        return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, ex.getMessage()).build();
    }

    @ExceptionHandler(InvalidDataException.class)
    @ApiResponse(
            responseCode = "400",
            description = "Validation error - invalid data",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleInvalidDataException(InvalidDataException ex) {
        return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, ex.getMessage()).build();
    }

    @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions (editing other member without admin permission, or accessing admin-only fields)",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ExceptionHandler(AuthorizationException.class)
    public ErrorResponse handleAuthorizationException(AuthorizationException ex) {
        return ErrorResponse.builder(ex, HttpStatus.FORBIDDEN, ex.getMessage()).title("Authorization Failed").build();
    }


    @ApiResponse(
            responseCode = "404",
            description = "Resource not found",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ExceptionHandler(ResourceNotFoundException.class)
    public ErrorResponse handleAuthorizationException(ResourceNotFoundException ex) {
        return ErrorResponse.builder(ex, HttpStatus.NOT_FOUND, ex.getMessage()).title("Resource Not Found").build();
    }

    // TODO: create SpringDoc filter which will remove 409 response status from GET APIs
    @ApiResponse(
            responseCode = "409",
            description = "Conflict - concurrent update (optimistic locking failure)",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ErrorResponse handleOptimisticLockingFailureException(OptimisticLockingFailureException ex) {
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, ex.getMessage()).title("Concurrent Update Conflict").build();
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var result = super.handleMethodArgumentNotValid(ex, headers, status, request);

        if (result.getBody() instanceof ProblemDetail problemDetail) {
            if (!ex.getFieldErrors().isEmpty()) {
                Map<String, String> errors = new HashMap<>();
                ex.getFieldErrors()
                        .forEach(fieldError -> errors.put(fieldError.getField(), fieldError.getDefaultMessage()));
                problemDetail.setProperty("fieldErrors", errors);
            }

            if (!ex.getGlobalErrors().isEmpty()) {
                problemDetail.setProperty("errors",
                        ex.getGlobalErrors()
                                .stream()
                                .map(e -> "%s [%s]".formatted(e.getDefaultMessage(), e.getCode()))
                                .toList());
            }
        }

        return result;
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var result = super.handleHandlerMethodValidationException(ex, headers, status, request);

        if (result.getBody() instanceof ProblemDetail problemDetail) {
            if (!ex.getAllErrors().isEmpty()) {
                problemDetail.setProperty("parameterErrors",
                        ex.getParameterValidationResults().stream().map(this::toMessage).toList());
            }
        }

        return result;
    }

    private String toMessage(ParameterValidationResult validationResult) {
        return "%s: %s".formatted(validationResult.getMethodParameter().getParameterName(),
                validationResult.getResolvableErrors().stream().map(MessageSourceResolvable::getDefaultMessage).collect(
                        Collectors.joining(", ")));
    }
}
