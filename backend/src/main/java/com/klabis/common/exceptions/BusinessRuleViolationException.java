package com.klabis.common.exceptions;

/**
 * Base exception for business rule violations.
 * <p>
 * Exceptions extending this class should be handled with HTTP 400 (Bad Request) status.
 * <p>
 * This exception indicates that a business rule or validation constraint was violated.
 * Examples include duplicate registrations, invalid updates, domain validation failures, etc.
 *
 * @see org.springframework.http.HttpStatus#BAD_REQUEST
 */
public abstract class BusinessRuleViolationException extends RuntimeException {

    protected BusinessRuleViolationException(String message) {
        super(message);
    }

    protected BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
