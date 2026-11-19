package com.klabis.membershipfees;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record MembershipFeeTypeId(UUID value) implements Identifier {

    public MembershipFeeTypeId {
        if (value == null) {
            throw new IllegalArgumentException("MembershipFeeTypeId value is required");
        }
    }


}
