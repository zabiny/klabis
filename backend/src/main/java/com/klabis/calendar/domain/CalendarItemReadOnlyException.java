package com.klabis.calendar.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class CalendarItemReadOnlyException extends BusinessRuleViolationException implements UserFacingError {

    public CalendarItemReadOnlyException() {
        super("Cannot manually modify event-linked calendar item. Event-linked items are read-only.");
    }
}
