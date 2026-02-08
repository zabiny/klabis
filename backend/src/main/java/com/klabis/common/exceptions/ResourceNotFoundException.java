package com.klabis.common.exceptions;

/**
 * Base exception for resource not found errors.
 * <p>
 * Exceptions extending this class should be handled with HTTP 404 (Not Found) status.
 * <p>
 * This exception indicates that a requested resource (member, event, registration, etc.)
 * could not be found in the system.
 *
 * @see org.springframework.http.HttpStatus#NOT_FOUND
 */
public abstract class ResourceNotFoundException extends RuntimeException {

    protected ResourceNotFoundException(String message) {
        super(message);
    }

    protected ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
