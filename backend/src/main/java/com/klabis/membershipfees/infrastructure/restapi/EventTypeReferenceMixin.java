package com.klabis.membershipfees.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.membershipfees.domain.EventTypeReference;
import org.springframework.boot.jackson.JacksonMixin;

import java.util.UUID;

@JacksonMixin(EventTypeReference.class)
public abstract class EventTypeReferenceMixin {
    @JsonValue
    abstract UUID value();

    @JsonCreator
    static EventTypeReference create(UUID value) {
        return EventTypeReference.of(value);
    }

}
