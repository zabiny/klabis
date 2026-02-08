package com.klabis.events;

import com.klabis.common.exceptions.AuthorizationException;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.ResourceNotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Exception handler for events module.
 * <p>
 * Handles exceptions specific to the events module, including:
 * <ul>
 *   <li>Resource not found (404) - EventNotFoundException, RegistrationNotFoundException</li>
 *   <li>Authorization errors (403) - MemberProfileRequiredException</li>
 *   <li>Business rule violations (400/409) - DuplicateRegistrationException, IllegalStateException</li>
 *   <li>Invalid arguments (400) - IllegalArgumentException</li>
 * </ul>
 * <p>
 * This handler has priority @Order(1) to ensure it catches module-specific exceptions
 * before they reach the CommonExceptionHandler.
 */
@RestControllerAdvice
@Order(1)  // Priority 1 - higher than CommonExceptionHandler (LOWEST_PRECEDENCE)
public class EventsExceptionHandler {

    /**
     * Handle resource not found exceptions (404).
     * <p>
     * Catches all exceptions extending {@link ResourceNotFoundException},
     * including EventNotFoundException and RegistrationNotFoundException.
     *
     * @param ex the resource not found exception
     * @return 404 Not Found with error details
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("https://klabis.com/problems/resource-not-found"));
        // Preserve specific title based on exception type for backward compatibility
        String title = getTitleForResourceNotFoundException(ex);
        problemDetail.setTitle(title);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Handle authorization exceptions (403).
     * <p>
     * Catches all exceptions extending {@link AuthorizationException},
     * including MemberProfileRequiredException.
     *
     * @param ex the authorization exception
     * @return 403 Forbidden with error details
     */
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ProblemDetail> handleAuthorizationException(AuthorizationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("https://klabis.com/problems/authorization-failed"));
        // Preserve specific title based on exception type for backward compatibility
        String title = ex.getClass().getSimpleName().equals("MemberProfileRequiredException")
                ? "Member Profile Required"
                : "Authorization Failed";
        problemDetail.setTitle(title);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    /**
     * Handle business rule violation exceptions (409).
     * <p>
     * Catches all exceptions extending {@link BusinessRuleViolationException},
     * including DuplicateRegistrationException.
     * <p>
     * Returns 409 Conflict for duplicate registrations (HTTP 409 is semantically
     * correct for "cannot create because already exists").
     *
     * @param ex the business rule violation exception
     * @return 409 Conflict with error details
     */
    @ExceptionHandler({BusinessRuleViolationException.class})
    public ResponseEntity<ProblemDetail> handleBusinessRuleViolationException(BusinessRuleViolationException ex) {
        // For duplicate registrations, return 409 Conflict
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("https://klabis.com/problems/business-rule-violation"));
        // Preserve specific title for DuplicateRegistrationException
        String title = ex.getClass().getSimpleName().equals("DuplicateRegistrationException")
                ? "Registration Conflict"
                : "Business Rule Violation";
        problemDetail.setTitle(title);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    /**
     * Get specific title for resource not found exceptions to preserve API compatibility.
     */
    private String getTitleForResourceNotFoundException(ResourceNotFoundException ex) {
        String simpleName = ex.getClass().getSimpleName();
        if (simpleName.contains("EventNotFoundException")) {
            return "Event Not Found";
        } else if (simpleName.contains("RegistrationNotFoundException")) {
            return "Registration Not Found";
        }
        return "Resource Not Found";
    }

    /**
     * Handle illegal state exceptions (400).
     * <p>
     * Catches IllegalStateException for business rule violations in event operations.
     *
     * @param ex the illegal state exception
     * @return 400 Bad Request with error details
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalStateException(IllegalStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("https://klabis.com/problems/illegal-state"));
        problemDetail.setTitle("Business Rule Violation");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handle illegal argument exceptions (400).
     * <p>
     * Catches IllegalArgumentException for domain validation errors.
     * Note: Messages are considered safe to expose as they contain domain validation rules.
     *
     * @param ex the illegal argument exception
     * @return 400 Bad Request with error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("https://klabis.com/problems/invalid-argument"));
        problemDetail.setTitle("Invalid Argument");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }
}
