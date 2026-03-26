package com.klabis.events.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.events.EventId;
import org.springframework.boot.jackson.JacksonMixin;

import java.util.UUID;

@JacksonMixin(EventId.class)
public abstract class EventIdMixin {

    @JsonValue
    abstract UUID value();

    @JsonCreator
    static EventId create(UUID value) {
        return new EventId(value);
    }
}
