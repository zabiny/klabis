package com.klabis.common.mvc;

import com.klabis.common.exceptions.AuthorizationException;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.usergroup.CannotPromoteNonMemberToOwnerException;
import com.klabis.common.usergroup.CannotRemoveLastOwnerException;
import com.klabis.common.usergroup.DirectMemberAdditionNotAllowedException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
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
import java.util.List;
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

    @ExceptionHandler(IllegalArgumentException.class)
    @ApiResponse(
            responseCode = "400",
            description = "Bad request - invalid argument",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, ex.getMessage()).build();
    }

    @ExceptionHandler(CannotRemoveLastOwnerException.class)
    @ApiResponse(
            responseCode = "422",
            description = "Unprocessable entity - cannot remove the last owner of a group",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleCannotRemoveLastOwner(CannotRemoveLastOwnerException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(422), ex.getMessage())
                .title("Cannot Remove Last Owner")
                .build();
    }

    @ExceptionHandler(DirectMemberAdditionNotAllowedException.class)
    @ApiResponse(
            responseCode = "422",
            description = "Unprocessable entity - direct member addition not allowed for invitation-based groups",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleDirectMemberAdditionNotAllowed(DirectMemberAdditionNotAllowedException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(422), ex.getMessage())
                .title("Direct Member Addition Not Allowed")
                .build();
    }

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

    @ExceptionHandler(CannotPromoteNonMemberToOwnerException.class)
    @ApiResponse(
            responseCode = "409",
            description = "Conflict - cannot promote a non-member to owner",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleCannotPromoteNonMemberToOwner(CannotPromoteNonMemberToOwnerException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(409), ex.getMessage())
                .title("Cannot Promote Non-Member to Owner")
                .build();
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
        return ErrorResponse.builder(ex, HttpStatus.NOT_FOUND, ex.getMessage())
                .title("Resource Not Found")
                .type(java.net.URI.create("about:blank"))
                .build();
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
            Map<String, String> fieldErrors = new HashMap<>();
            List<String> parameterErrors = new java.util.ArrayList<>();

            for (ParameterValidationResult pvr : ex.getParameterValidationResults()) {
                List<? extends MessageSourceResolvable> resolvableErrors = pvr.getResolvableErrors();
                boolean hasFieldErrors = resolvableErrors.stream().anyMatch(e -> e instanceof FieldError);

                if (hasFieldErrors) {
                    resolvableErrors.stream()
                            .filter(e -> e instanceof FieldError)
                            .map(e -> (FieldError) e)
                            .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
                } else {
                    String message = toMessage(pvr);
                    if (!message.isBlank()) {
                        parameterErrors.add(message);
                    }
                }
            }

            if (!fieldErrors.isEmpty()) {
                problemDetail.setProperty("fieldErrors", fieldErrors);
            }
            if (!parameterErrors.isEmpty()) {
                problemDetail.setProperty("parameterErrors", parameterErrors);
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
