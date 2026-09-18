package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

/**
 * An opaque, cheap change indicator from the external system (design.md D3).
 * <p>
 * When unchanged since the last pass, the engine may skip a full external read.
 * Not every integration offers one — {@link SyncCapabilities} does not model its
 * presence explicitly; an adapter without a token simply always returns
 * {@link java.util.Optional#empty()} from
 * {@link SynchronizationAdapter#externalVersion(String)}.
 */
@ValueObject
public record ExternalVersionToken(String value) {

    public ExternalVersionToken {
        Assert.hasText(value, "value is required");
    }
}
