package com.klabis.members.management;

import com.klabis.common.exceptions.InvalidDataException;

/**
 * Exception thrown when an update request fails validation.
 * <p>
 * This exception is thrown when the update data violates business rules
 * or validation constraints.
 */
public class InvalidUpdateException extends InvalidDataException {

    public InvalidUpdateException(String message) {
        super(message);
    }

    public InvalidUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
