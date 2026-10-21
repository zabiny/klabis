package com.klabis.events.domain;

import com.klabis.common.exceptions.ConflictException;

public class EventTypeNameAlreadyExistsException extends ConflictException {

    public EventTypeNameAlreadyExistsException(String name) {
        super("Event type with name '" + name + "' already exists");
    }
}
