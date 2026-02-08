package com.klabis.common.exceptions;

/**
 * Base exception for authorization errors.
 * <p>
 * Exceptions extending this class should be handled with HTTP 403 (Forbidden) status.
 * <p>
 * This exception indicates that the authenticated user lacks sufficient permissions
 * to perform the requested operation. This is distinct from authentication failures
 * (HTTP 401) and should be used when the user is logged in but not authorized.
 *
 * @see org.springframework.http.HttpStatus#FORBIDDEN
 */
public abstract class AuthorizationException extends RuntimeException {

    protected AuthorizationException(String message) {
        super(message);
    }

    protected AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
