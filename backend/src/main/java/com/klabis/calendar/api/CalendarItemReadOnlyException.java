package com.klabis.calendar.api;

import com.klabis.common.exceptions.BusinessRuleViolationException;

/**
 * Exception thrown when attempting to manually modify an event-linked calendar item.
 * <p>
 * Event-linked calendar items (eventId != null) are read-only and managed
 * automatically through event handlers. Manual updates or deletions are not allowed.
 * <p>
 * This exception should be mapped to HTTP 400 (Bad Request) status.
 */
class CalendarItemReadOnlyException extends BusinessRuleViolationException {

    CalendarItemReadOnlyException() {
        super("Cannot manually modify event-linked calendar item. Event-linked items are read-only.");
    }
}
