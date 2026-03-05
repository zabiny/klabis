package com.klabis.common.exceptions;

/**
 * Base exception for data validation violations.
 * <p>
 * Exceptions extending this class should be handled with HTTP 400 (Bad Request) status.
 * <p>
 *
 * @see org.springframework.http.HttpStatus#BAD_REQUEST
 */
public abstract class InvalidDataException extends RuntimeException {

    protected InvalidDataException(String message) {
        super(message);
    }

    protected InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
