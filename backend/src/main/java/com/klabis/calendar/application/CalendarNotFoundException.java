package com.klabis.calendar.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.UUID;

public class CalendarNotFoundException extends ResourceNotFoundException implements UserFacingError {

    public CalendarNotFoundException(UUID calendarItemId) {
        super("Calendar item not found with ID: " + calendarItemId);
    }

    @Override
    public String errorType() {
        return "calendar-item-not-found";
    }
}
