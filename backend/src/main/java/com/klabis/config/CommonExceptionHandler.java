package com.klabis.config;

import com.klabis.common.ratelimit.RateLimitExceededException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Common exception handler for REST API.
 * <p>
 * Uses RFC 7807 Problem Details for HTTP APIs (application/problem+json).
 * <p>
 * Security: Exception details are only exposed in non-production environments.
 * In production, generic messages are returned to prevent information disclosure.
 * <p>
 * This handler has LOWEST_PRECEDENCE order, allowing module-specific handlers
 * (MembersExceptionHandler, EventsExceptionHandler) to handle their exceptions first.
 *
 * @see com.klabis.members.management.MembersExceptionHandler
 * @see com.klabis.events.EventsExceptionHandler
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class CommonExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CommonExceptionHandler.class);

    private final Environment environment;

    private static final String PROD_PROFILE = "prod";

    public CommonExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    /**
     * Check if the application is running in production mode.
     *
     * @return true if production profile is active
     */
    private boolean isProduction() {
        return Arrays.asList(environment.getActiveProfiles()).contains(PROD_PROFILE);
    }


    /**
     * Handles rate limit exceeded exceptions.
     *
     * <p>This returns HTTP 429 (Too Many Requests) with a Retry-After header
     * indicating when the client may retry the request.
     *
     * <p>The rate limit is configured in {@code application.yml} under
     * {@code password-setup.rate-limit} and defaults to 3 requests per hour
     * per registration number.
     *
     * @param ex the rate limit exception
     * @return RFC 7807 ProblemDetail with rate limit information
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(RateLimitExceededException ex) {

        log.warn("Rate limit exceeded: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again later."
        );
        problemDetail.setType(URI.create("https://klabis.com/problems/rate-limit-exceeded"));
        problemDetail.setTitle("Too Many Requests");

        // Set Retry-After header to 1 hour (3600 seconds)
        // This matches the rate limit refresh period in application.yml
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "3600")
                .body(problemDetail);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        // In production, don't expose detailed authentication errors
        String detailMessage = isProduction()
                ? "Authentication required"
                : (ex.getMessage() != null ? ex.getMessage() : "Authentication required");

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                detailMessage
        );
        problemDetail.setType(URI.create("https://api.klabis.example.com/errors/unauthorized"));
        problemDetail.setTitle("Unauthorized");

        // Include stack trace in non-production environments for debugging
        if (!isProduction()) {
            problemDetail.setProperty("debug_message", ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied for user: {}", ex.getMessage());

        // In production, don't expose detailed authorization errors
        String detailMessage = isProduction()
                ? "Insufficient authority"
                : "Access Denied: " + (ex.getMessage() != null ? ex.getMessage() : "Insufficient authority");

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                detailMessage
        );
        problemDetail.setType(URI.create("https://api.klabis.example.com/errors/forbidden"));
        problemDetail.setTitle("Forbidden");

        // Include stack trace in non-production environments for debugging
        if (!isProduction()) {
            problemDetail.setProperty("debug_message", ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    /**
     * Handle validation errors from @Valid annotation.
     *
     * @param ex the validation exception
     * @return problem detail with validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for one or more fields"
        );

        problemDetail.setType(URI.create("https://klabis.com/problems/validation-error"));
        problemDetail.setTitle("Validation Error");

        // Collect all field errors
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        problemDetail.setProperty("errors", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetail);
    }

    /**
     * Handle missing required request parameters.
     * <p>
     * This occurs when a required @RequestParam is not present in the request.
     *
     * @param ex the missing parameter exception
     * @return problem detail with parameter name
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.debug("Missing required request parameter: {}", ex.getParameterName());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                String.format("Required parameter '%s' is missing", ex.getParameterName())
        );

        problemDetail.setType(URI.create("https://klabis.com/problems/missing-parameter"));
        problemDetail.setTitle("Missing Required Parameter");
        problemDetail.setProperty("parameter_name", ex.getParameterName());
        problemDetail.setProperty("parameter_type", ex.getParameterType());

        return problemDetail;
    }

    /**
     * Handle rate limit exceeded exceptions from resilience4j.
     *
     * <p>This returns HTTP 429 (Too Many Requests) with a Retry-After header
     * indicating when the client may retry the request.
     *
     * @param ex the rate limit exception
     * @return problem detail with rate limit information
     */
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ProblemDetail> handleRequestNotPermitted(RequestNotPermitted ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again later."
        );

        problemDetail.setType(URI.create("https://klabis.com/problems/rate-limit-exceeded"));
        problemDetail.setTitle("Too Many Requests");

        // Set Retry-After header to 1 hour (3600 seconds)
        // This matches the rate limit refresh period in application.yml
        ResponseEntity<ProblemDetail> response = ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "3600")
                .body(problemDetail);

        return response;
    }

    /**
     * Handle generic exceptions.
     * <p>
     * Security: In production, no exception details are exposed to prevent
     * information disclosure. All exceptions are logged with ERROR level.
     *
     * @param ex the exception
     * @return problem detail
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        // Always log the full exception for debugging/monitoring
        log.error("Unexpected error occurred", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );

        problemDetail.setType(URI.create("https://klabis.com/problems/internal-error"));
        problemDetail.setTitle("Internal Server Error");

        // In non-production environments, include debug information
        if (!isProduction()) {
            if (ex.getMessage() != null) {
                problemDetail.setProperty("message", ex.getMessage());
            }
            problemDetail.setProperty("exception_type", ex.getClass().getSimpleName());
        }

        return problemDetail;
    }
}
