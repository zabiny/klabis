package com.klabis.membershipfees;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record FeeYearPublicationId(UUID value) implements Identifier {

    public FeeYearPublicationId {
        if (value == null) {
            throw new IllegalArgumentException("FeeYearPublicationId value is required");
        }
    }

    public UUID uuid() {
        return this.value;
    }
}
