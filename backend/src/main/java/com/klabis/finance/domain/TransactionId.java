package com.klabis.finance.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record TransactionId(UUID value) implements Identifier {

    public static TransactionId newId() {
        return new TransactionId(UUID.randomUUID());
    }
}
