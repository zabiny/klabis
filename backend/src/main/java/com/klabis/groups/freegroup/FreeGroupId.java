package com.klabis.groups.freegroup;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record FreeGroupId(UUID value) implements Identifier {

    public UUID uuid() {
        return this.value;
    }
}
