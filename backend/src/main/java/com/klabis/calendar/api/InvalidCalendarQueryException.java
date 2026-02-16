package com.klabis.calendar.api;

import com.klabis.common.exceptions.InvalidDataException;

/**
 * Exception thrown when calendar item query parameters are invalid.
 * <p>
 * Maps to HTTP 400 Bad Request status.
 */
class InvalidCalendarQueryException extends InvalidDataException {

    InvalidCalendarQueryException(String message) {
        super(message);
    }
}
