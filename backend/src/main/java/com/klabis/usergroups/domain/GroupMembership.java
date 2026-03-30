package com.klabis.usergroups.domain;

import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Instant;

@ValueObject
public record GroupMembership(MemberId memberId, Instant joinedAt) {

    public static GroupMembership of(MemberId memberId) {
        return new GroupMembership(memberId, Instant.now());
    }
}
