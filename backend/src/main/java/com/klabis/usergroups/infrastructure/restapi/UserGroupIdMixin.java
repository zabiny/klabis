package com.klabis.usergroups.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.usergroups.UserGroupId;
import org.springframework.boot.jackson.JacksonMixin;

import java.util.UUID;

@JacksonMixin(UserGroupId.class)
public abstract class UserGroupIdMixin {

    @JsonValue
    abstract UUID value();

    @JsonCreator
    static UserGroupId create(UUID value) {
        return new UserGroupId(value);
    }
}
