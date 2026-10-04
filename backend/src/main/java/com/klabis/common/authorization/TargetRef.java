package com.klabis.common.authorization;

import org.springframework.util.Assert;

import java.util.UUID;

/**
 * The object an action or field is about, identified by its kind and id.
 */
public record TargetRef(TargetType type, UUID id) {

    public TargetRef {
        Assert.notNull(type, "Target type must not be null");
        Assert.isTrue(type != TargetType.NONE, "TargetType.NONE does not describe a target");
        Assert.notNull(id, "Target id must not be null");
    }

    public static TargetRef member(UUID id) {
        return new TargetRef(TargetType.MEMBER, id);
    }

    public static TargetRef event(UUID id) {
        return new TargetRef(TargetType.EVENT, id);
    }
}
