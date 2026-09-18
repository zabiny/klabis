package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

/**
 * The reference pair for change detection: the local and external snapshot captured
 * at the last reconciliation (design.md D4).
 * <p>
 * Identical halves after an ordinary synchronisation. Diverged halves record a
 * standing accepted divergence (D6) and are what makes the inward guard of D4 possible:
 * while diverged, any further external movement is a conflict rather than a silent
 * overwrite of the accepted local value.
 */
@ValueObject
public record SyncBaseline(SyncSnapshot local, SyncSnapshot external) {

    public SyncBaseline {
        Assert.notNull(local, "local is required");
        Assert.notNull(external, "external is required");
    }

    /**
     * An ordinary reconciliation: both baseline halves are set to the same snapshot,
     * since the two sides have just been brought into agreement.
     */
    public static SyncBaseline reconciled(SyncSnapshot snapshot) {
        return new SyncBaseline(snapshot, snapshot);
    }

    /**
     * An accepted divergence (design.md D6): the baseline pair is set to the current,
     * differing snapshots of each side, so the divergence is recorded as deliberate.
     */
    public static SyncBaseline accepted(SyncSnapshot local, SyncSnapshot external) {
        return new SyncBaseline(local, external);
    }

    public boolean isDiverged() {
        return !local.matches(external);
    }
}
