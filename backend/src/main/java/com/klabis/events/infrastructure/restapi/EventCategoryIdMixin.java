package com.klabis.events.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.events.EventCategoryId;
import org.springframework.boot.jackson.JacksonMixin;

import java.util.UUID;

@JacksonMixin(EventCategoryId.class)
public abstract class EventCategoryIdMixin {

    @JsonValue
    abstract UUID value();

    @JsonCreator
    static EventCategoryId create(UUID value) {
        return new EventCategoryId(value);
    }
}
