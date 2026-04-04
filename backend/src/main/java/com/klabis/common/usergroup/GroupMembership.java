package com.klabis.common.usergroup;

import com.klabis.common.users.UserId;
import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Instant;

@ValueObject
public record GroupMembership(UserId userId, Instant joinedAt) {

    public static GroupMembership of(UserId userId) {
        return new GroupMembership(userId, Instant.now());
    }
}
