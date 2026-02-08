package com.klabis.members.management;

import com.klabis.common.exceptions.BusinessRuleViolationException;

/**
 * Exception thrown when an update request fails validation.
 * <p>
 * This exception is thrown when the update data violates business rules
 * or validation constraints.
 */
class InvalidUpdateException extends BusinessRuleViolationException {

    public InvalidUpdateException(String message) {
        super(message);
    }

    public InvalidUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
