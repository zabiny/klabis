package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.time.Instant;

/**
 * A manager's confirmation that they have seen a specific collision, bound to the hash
 * pair current at that moment (design.md D7). Introduced here as part of the target
 * shape of {@link SyncRecord}; populated starting with the conflicts slice.
 */
@ValueObject
public record ConflictAcknowledgement(
        SyncHash acknowledgedLocalHash,
        SyncHash acknowledgedExternalHash,
        Instant acknowledgedAt,
        String acknowledgedBy
) {

    public ConflictAcknowledgement {
        Assert.notNull(acknowledgedLocalHash, "acknowledgedLocalHash is required");
        Assert.notNull(acknowledgedExternalHash, "acknowledgedExternalHash is required");
        Assert.notNull(acknowledgedAt, "acknowledgedAt is required");
        Assert.hasText(acknowledgedBy, "acknowledgedBy is required");
    }

    /**
     * Whether this acknowledgement still applies to the record's current hash pair
     * (design.md D7) — a resolution never trusts stored snapshots without this check.
     */
    public boolean isCurrentFor(SyncHash currentLocalHash, SyncHash currentExternalHash) {
        return acknowledgedLocalHash.equals(currentLocalHash) && acknowledgedExternalHash.equals(currentExternalHash);
    }
}
