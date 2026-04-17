package com.klabis.groups.freegroup.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.groups.freegroup.FreeGroupId;
import org.springframework.boot.jackson.JacksonMixin;

import java.util.UUID;

@JacksonMixin(FreeGroupId.class)
public abstract class FreeGroupIdMixin {

    @JsonValue
    abstract UUID value();

    @JsonCreator
    static FreeGroupId create(UUID value) {
        return new FreeGroupId(value);
    }
}
