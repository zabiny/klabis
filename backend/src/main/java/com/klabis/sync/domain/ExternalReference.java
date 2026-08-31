package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

/**
 * The counterpart identity in the external system.
 * <p>
 * Held only by the synchronisation record — the paired Klabis entity gains no new
 * field for it (design.md D2). {@code externalId} is a single opaque string; an
 * integration whose external identity is composite (e.g. an ORIS person plus a
 * club-membership id) encodes both into this one string.
 */
@ValueObject
public record ExternalReference(ExternalSystem system, String externalId) {

    public ExternalReference {
        Assert.notNull(system, "system is required");
        Assert.hasText(externalId, "externalId is required");
    }
}
