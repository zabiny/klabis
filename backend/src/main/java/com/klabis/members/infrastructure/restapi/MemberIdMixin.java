package com.klabis.members.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.members.MemberId;
import org.springframework.boot.jackson.JacksonMixin;

import java.util.UUID;

@JacksonMixin(MemberId.class)
public abstract class MemberIdMixin {

    @JsonValue
    abstract UUID value();

    @JsonCreator
    static MemberId create(UUID value) {
        return new MemberId(value);
    }
}
