package com.klabis.common.mvc;

import com.klabis.common.exceptions.AuthorizationException;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.CommonErrorTypes;
import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.util.Assert;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static final Logger LOG = LoggerFactory.getLogger(MvcExceptionHandler.class);

    private static final String INVALID_REQUEST_DETAIL = "Invalid request";

    private static final Set<String> RESERVED_PROPERTIES =
            Set.of("type", "title", "status", "detail", "instance", "field");

    private static ErrorResponse toErrorResponse(Exception ex, HttpStatus status, String genericType, String title) {
        ErrorResponse.Builder builder = ErrorResponse.builder(ex, status, ex.getMessage()).title(title);
        if (ex instanceof UserFacingError userFacing) {
            builder.type(URI.create(CommonErrorTypes.uri(userFacing.errorType())));
            userFacing.messageParameters().forEach((name, value) -> {
                Assert.isTrue(!RESERVED_PROPERTIES.contains(name),
                        () -> "Message parameter name collides with a ProblemDetail property: " + name);
                builder.property(name, value);
            });
            if (userFacing.field() != null) {
                builder.property("field", userFacing.field());
            }
        } else {
            builder.type(URI.create(genericType));
        }
        return builder.build();
    }

    private static ErrorResponse internalCheckFailed(RuntimeException ex) {
        if (ex instanceof IllegalStateException) {
            LOG.error("Internal state check failed: {}", ex.getMessage(), ex);
        } else {
            LOG.warn("Internal check failed: {}", ex.getMessage(), ex);
        }
        return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, INVALID_REQUEST_DETAIL)
                .type(URI.create(CommonErrorTypes.INVALID_REQUEST))
                .build();
    }

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
        return internalCheckFailed(ex);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ApiResponse(
            responseCode = "400",
            description = "Bad request - invalid state",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleIllegalStateException(IllegalStateException ex) {
        return internalCheckFailed(ex);
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
        return toErrorResponse(ex, HttpStatus.BAD_REQUEST, CommonErrorTypes.INVALID_REQUEST, "Bad Request");
    }

    @ExceptionHandler(ConflictException.class)
    @ApiResponse(
            responseCode = "409",
            description = "Conflict - request conflicts with the current state of the resource",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleConflictException(ConflictException ex) {
        return toErrorResponse(ex, HttpStatus.CONFLICT, CommonErrorTypes.CONFLICT, "Conflict");
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
        return toErrorResponse(ex, HttpStatus.BAD_REQUEST, CommonErrorTypes.INVALID_REQUEST, "Bad Request");
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
        return toErrorResponse(ex, HttpStatus.FORBIDDEN, CommonErrorTypes.FORBIDDEN, "Authorization Failed");
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
    public ErrorResponse handleResourceNotFoundException(ResourceNotFoundException ex) {
        return toErrorResponse(ex, HttpStatus.NOT_FOUND, CommonErrorTypes.NOT_FOUND, "Resource Not Found");
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
        return ErrorResponse.builder(ex, HttpStatus.CONFLICT, ex.getMessage())
                .title("Concurrent Update Conflict")
                .type(URI.create(CommonErrorTypes.CONCURRENT_UPDATE))
                .build();
    }

    // A generated *Api interface is class-level @Validated, so a constrained @RequestParam (e.g. a
    // required, pattern-checked query parameter) is validated by AOP's MethodValidationInterceptor
    // ahead of Spring MVC's own argument resolution, and fails with this exception instead of
    // HandlerMethodValidationException below. No hand-written controller parameter hit this before
    // spec-first generation started using @Validated interfaces.
    @ApiResponse(
            responseCode = "400",
            description = "Bad request - invalid argument",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException ex) {
        List<String> parameterErrors = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .toList();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setType(URI.create(CommonErrorTypes.VALIDATION_FAILED));
        if (!parameterErrors.isEmpty()) {
            problemDetail.setProperty("parameterErrors", parameterErrors);
        }
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> result = super.handleExceptionInternal(ex, body, headers, statusCode, request);

        if (result != null && result.getBody() instanceof ProblemDetail problemDetail
                && isDefaultType(problemDetail.getType())) {
            String type = genericTypeFor(ex, statusCode);
            if (type != null) {
                problemDetail.setType(URI.create(type));
            }
        }
        return result;
    }

    private static boolean isDefaultType(@Nullable URI type) {
        return type == null || "about:blank".equals(type.toString());
    }

    private static @Nullable String genericTypeFor(Exception ex, HttpStatusCode status) {
        if (ex instanceof MethodArgumentNotValidException || ex instanceof HandlerMethodValidationException) {
            return CommonErrorTypes.VALIDATION_FAILED;
        }
        return switch (status.value()) {
            case 400 -> CommonErrorTypes.INVALID_REQUEST;
            case 404 -> CommonErrorTypes.NOT_FOUND;
            case 409 -> CommonErrorTypes.CONFLICT;
            default -> status.is5xxServerError() ? CommonErrorTypes.UNEXPECTED : null;
        };
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
                                .map(ObjectError::getDefaultMessage)
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
        return validationResult.getResolvableErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
    }
}
