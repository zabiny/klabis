package com.klabis.members.legalguardiangroup;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record LegalGuardianGroupId(UUID value) implements Identifier {

    public UUID uuid() {
        return this.value;
    }
}
