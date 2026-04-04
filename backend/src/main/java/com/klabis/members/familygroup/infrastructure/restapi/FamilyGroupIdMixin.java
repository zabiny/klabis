package com.klabis.members.familygroup.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.members.familygroup.domain.FamilyGroupId;
import org.springframework.boot.jackson.JacksonMixin;

import java.util.UUID;

@JacksonMixin(FamilyGroupId.class)
public abstract class FamilyGroupIdMixin {

    @JsonValue
    abstract UUID value();

    @JsonCreator
    static FamilyGroupId create(UUID value) {
        return new FamilyGroupId(value);
    }
}
