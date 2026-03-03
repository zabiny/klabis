package com.klabis.calendar.application;

import com.klabis.common.exceptions.ResourceNotFoundException;

import java.util.UUID;

public class CalendarNotFoundException extends ResourceNotFoundException {

    public CalendarNotFoundException(UUID calendarItemId) {
        super("Calendar item not found with ID: " + calendarItemId);
    }
}
