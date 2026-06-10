package com.klabis.membershipfees;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record MembershipFeeGroupId(UUID value) implements Identifier {

    public MembershipFeeGroupId {
        if (value == null) {
            throw new IllegalArgumentException("MembershipFeeGroupId value is required");
        }
    }


}
