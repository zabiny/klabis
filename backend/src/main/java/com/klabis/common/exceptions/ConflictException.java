package com.klabis.common.exceptions;

/**
 * Base exception for business rule violations caused by a conflict with the current state of a resource.
 * <p>
 * Exceptions extending this class should be handled with HTTP 409 (Conflict) status.
 *
 * @see org.springframework.http.HttpStatus#CONFLICT
 */
public abstract class ConflictException extends BusinessRuleViolationException {

    protected ConflictException(String message) {
        super(message);
    }

    protected ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
