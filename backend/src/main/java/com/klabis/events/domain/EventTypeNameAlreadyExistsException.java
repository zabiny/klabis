package com.klabis.events.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

public class EventTypeNameAlreadyExistsException extends ConflictException implements UserFacingError {

    private final String name;


    public EventTypeNameAlreadyExistsException(String name) {
        super("Event type with name '" + name + "' already exists");
        this.name = name;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("name", name);
    }

    @Override
    public String field() {
        return "name";
    }
}
