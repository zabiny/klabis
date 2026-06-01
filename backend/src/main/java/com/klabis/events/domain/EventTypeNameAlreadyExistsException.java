package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class EventTypeNameAlreadyExistsException extends BusinessRuleViolationException {

    public EventTypeNameAlreadyExistsException(String name) {
        super("Event type with name '" + name + "' already exists");
    }
}
