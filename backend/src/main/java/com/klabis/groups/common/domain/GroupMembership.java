package com.klabis.groups.common.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Instant;

@ValueObject
public record GroupMembership<M>(M memberId, Instant joinedAt) {

    public static <M> GroupMembership<M> of(M memberId) {
        return new GroupMembership<>(memberId, Instant.now());
    }
}
