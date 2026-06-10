package com.klabis.membershipfees;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record MembershipFeeTierId(UUID value) implements Identifier {

    public MembershipFeeTierId {
        if (value == null) {
            throw new IllegalArgumentException("MembershipFeeTierId value is required");
        }
    }


}
