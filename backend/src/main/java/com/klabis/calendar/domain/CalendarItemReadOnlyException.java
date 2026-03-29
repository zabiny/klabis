package com.klabis.calendar.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class CalendarItemReadOnlyException extends BusinessRuleViolationException {

    public CalendarItemReadOnlyException() {
        super("Cannot manually modify event-linked calendar item. Event-linked items are read-only.");
    }
}
