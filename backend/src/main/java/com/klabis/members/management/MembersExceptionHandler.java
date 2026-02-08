package com.klabis.members.management;

import com.klabis.common.exceptions.AuthorizationException;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.ResourceNotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Exception handler for members module.
 * <p>
 * Handles exceptions specific to the members module, including:
 * <ul>
 *   <li>Resource not found (404) - MemberNotFoundException</li>
 *   <li>Authorization errors (403) - SelfEditNotAllowedException, AdminFieldAccessException</li>
 *   <li>Authentication errors (401) - UserIdentificationException</li>
 *   <li>Business rule violations (400) - InvalidUpdateException</li>
 *   <li>Concurrent updates (409) - OptimisticLockingFailureException</li>
 * </ul>
 * <p>
 * This handler has priority @Order(1) to ensure it catches module-specific exceptions
 * before they reach the CommonExceptionHandler.
 */
@RestControllerAdvice
@Order(1)  // Priority 1 - higher than CommonExceptionHandler (LOWEST_PRECEDENCE)
class MembersExceptionHandler {

    /**
     * Handle resource not found exceptions (404).
     * <p>
     * Catches all exceptions extending {@link ResourceNotFoundException},
     * including MemberNotFoundException.
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
        String title = ex.getClass().getSimpleName().equals("MemberNotFoundException")
                ? "Member Not Found"
                : "Resource Not Found";
        problemDetail.setTitle(title);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Handle authorization exceptions (403).
     * <p>
     * Catches all exceptions extending {@link AuthorizationException},
     * including SelfEditNotAllowedException and AdminFieldAccessException.
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
        String title = getTitleForAuthorizationException(ex);
        problemDetail.setTitle(title);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    /**
     * Handle UserIdentificationException specifically (401).
     * <p>
     * This exception is thrown when the user cannot be identified from authentication.
     * Returns 401 Unauthorized (distinct from 403 Forbidden).
     * <p>
     * This handler must be defined before handleBusinessRuleViolationException
     * to ensure it takes precedence for UserIdentificationException.
     *
     * @param ex the user identification exception
     * @return 401 Unauthorized with error details
     */
    @ExceptionHandler(UserIdentificationException.class)
    public ResponseEntity<ProblemDetail> handleUserIdentificationException(UserIdentificationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("https://klabis.com/problems/user-identification-failed"));
        problemDetail.setTitle("Authentication Failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    /**
     * Handle business rule violation exceptions (400).
     * <p>
     * Catches all exceptions extending {@link BusinessRuleViolationException},
     * including InvalidUpdateException.
     *
     * @param ex the business rule violation exception
     * @return 400 Bad Request with error details
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRuleViolationException(BusinessRuleViolationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setType(URI.create("https://klabis.com/problems/business-rule-violation"));
        // Preserve specific title based on exception type for backward compatibility
        String title = getTitleForBusinessRuleException(ex);
        problemDetail.setTitle(title);
        return ResponseEntity.badRequest().body(problemDetail);
    }

    /**
     * Get specific title for authorization exceptions to preserve API compatibility.
     */
    private String getTitleForAuthorizationException(AuthorizationException ex) {
        String simpleName = ex.getClass().getSimpleName();
        return switch (simpleName) {
            case "SelfEditNotAllowedException" -> "Self-Edit Not Allowed";
            case "AdminFieldAccessException" -> "Admin Field Access Denied";
            default -> "Authorization Failed";
        };
    }

    /**
     * Get specific title for business rule violation exceptions to preserve API compatibility.
     */
    private String getTitleForBusinessRuleException(BusinessRuleViolationException ex) {
        String simpleName = ex.getClass().getSimpleName();
        return switch (simpleName) {
            case "InvalidUpdateException" -> "Invalid Update";
            default -> "Business Rule Violation";
        };
    }

    /**
     * Handle optimistic locking failures (409).
     * <p>
     * This occurs when two users try to update the same member concurrently.
     *
     * @param ex the optimistic locking exception
     * @return 409 Conflict with error details
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLockingFailureException(OptimisticLockingFailureException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "This member was modified by another user. Please refresh and try again."
        );
        problemDetail.setType(URI.create("https://klabis.com/problems/concurrent-update"));
        problemDetail.setTitle("Concurrent Update Conflict");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
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
        problemDetail.setTitle("Invalid Request");
        return ResponseEntity.badRequest().body(problemDetail);
    }
}
