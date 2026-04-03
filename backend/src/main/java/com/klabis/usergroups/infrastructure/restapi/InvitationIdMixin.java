package com.klabis.usergroups.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.usergroups.domain.InvitationId;
import org.springframework.boot.jackson.JacksonMixin;

import java.util.UUID;

@JacksonMixin(InvitationId.class)
public abstract class InvitationIdMixin {

    @JsonValue
    abstract UUID value();

    @JsonCreator
    static InvitationId create(UUID value) {
        return new InvitationId(value);
    }
}
