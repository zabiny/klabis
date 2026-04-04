package com.klabis.members.traininggroup.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record TrainingGroupId(UUID value) implements Identifier {

    public UUID uuid() {
        return this.value;
    }
}
