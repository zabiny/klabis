package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import org.springframework.boot.jackson.JacksonMixin;

import java.util.UUID;

@JacksonMixin(LegalGuardianGroupId.class)
public abstract class LegalGuardianGroupIdMixin {

    @JsonValue
    abstract UUID value();

    @JsonCreator
    static LegalGuardianGroupId create(UUID value) {
        return new LegalGuardianGroupId(value);
    }
}
