package com.klabis.calendar.application;

import com.klabis.common.exceptions.InvalidDataException;

public class InvalidCalendarQueryException extends InvalidDataException {

    public InvalidCalendarQueryException(String message) {
        super(message);
    }
}
