package com.klabis.events.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.events.CategoryPresetId;
import org.springframework.boot.jackson.JacksonMixin;

import java.util.UUID;

@JacksonMixin(CategoryPresetId.class)
public abstract class CategoryPresetIdMixin {

    @JsonValue
    abstract UUID value();

    @JsonCreator
    static CategoryPresetId create(UUID value) {
        return new CategoryPresetId(value);
    }
}
