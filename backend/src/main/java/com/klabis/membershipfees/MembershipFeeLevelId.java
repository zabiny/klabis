package com.klabis.membershipfees;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record MembershipFeeLevelId(UUID value) implements Identifier {

    public MembershipFeeLevelId {
        if (value == null) {
            throw new IllegalArgumentException("MembershipFeeLevelId value is required");
        }
    }

    public UUID uuid() {
        return this.value;
    }
}
