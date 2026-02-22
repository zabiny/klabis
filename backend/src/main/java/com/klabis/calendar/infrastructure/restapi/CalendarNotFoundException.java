package com.klabis.calendar.infrastructure.restapi;

import com.klabis.common.exceptions.ResourceNotFoundException;

import java.util.UUID;

/**
 * Exception thrown when a calendar item is not found.
 * <p>
 * Maps to HTTP 404 Not Found status.
 */
class CalendarNotFoundException extends ResourceNotFoundException {

    CalendarNotFoundException(UUID calendarItemId) {
        super("Calendar item not found with ID: " + calendarItemId);
    }
}
