package com.klabis.groups.membersgroup.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.groups.membersgroup.MembersGroupId;
import org.springframework.boot.jackson.JacksonMixin;

import java.util.UUID;

@JacksonMixin(MembersGroupId.class)
public abstract class MembersGroupIdMixin {

    @JsonValue
    abstract UUID value();

    @JsonCreator
    static MembersGroupId create(UUID value) {
        return new MembersGroupId(value);
    }
}
