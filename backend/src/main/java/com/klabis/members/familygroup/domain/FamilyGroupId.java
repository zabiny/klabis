package com.klabis.members.familygroup.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record FamilyGroupId(UUID value) implements Identifier {

    public UUID uuid() {
        return this.value;
    }
}
