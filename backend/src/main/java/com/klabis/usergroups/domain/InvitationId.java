package com.klabis.usergroups.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record InvitationId(UUID value) implements Identifier {

    public static InvitationId newId() {
        return new InvitationId(UUID.randomUUID());
    }
}
