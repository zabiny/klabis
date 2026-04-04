package com.klabis.members.membersgroup.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record MembersGroupId(UUID value) implements Identifier {

    public UUID uuid() {
        return this.value;
    }
}
