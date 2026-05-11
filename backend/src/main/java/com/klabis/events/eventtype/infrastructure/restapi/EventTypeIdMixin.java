package com.klabis.events.eventtype.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.events.EventTypeId;
import org.springframework.boot.jackson.JacksonMixin;

import java.util.UUID;

@JacksonMixin(EventTypeId.class)
public abstract class EventTypeIdMixin {

    @JsonValue
    abstract UUID value();

    @JsonCreator
    static EventTypeId create(UUID value) {
        return new EventTypeId(value);
    }
}
