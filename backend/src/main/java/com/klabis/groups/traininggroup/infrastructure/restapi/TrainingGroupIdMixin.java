package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.groups.traininggroup.TrainingGroupId;
import org.springframework.boot.jackson.JacksonMixin;

import java.util.UUID;

@JacksonMixin(TrainingGroupId.class)
public abstract class TrainingGroupIdMixin {

    @JsonValue
    abstract UUID value();

    @JsonCreator
    static TrainingGroupId create(UUID value) {
        return new TrainingGroupId(value);
    }
}
