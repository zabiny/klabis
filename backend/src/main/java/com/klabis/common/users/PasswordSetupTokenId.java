package com.klabis.common.users;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

/**
 * Value object representing a unique password setup token identifier.
 */
@ValueObject
public record PasswordSetupTokenId(UUID uuid) implements Identifier {

    public PasswordSetupTokenId {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
    }

    public static PasswordSetupTokenId newId() {
        return new PasswordSetupTokenId(UUID.randomUUID());
    }
}
