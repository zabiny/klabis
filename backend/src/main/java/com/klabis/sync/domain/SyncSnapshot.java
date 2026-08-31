package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

/**
 * A projection together with its own hash, as one indivisible value (design.md D4).
 * <p>
 * A hash that does not belong to its projection is the one inconsistency that would
 * silently corrupt every decision the engine makes, so the domain makes it
 * unrepresentable: the only way to build a snapshot is {@link #of(SyncProjection)},
 * which always computes the hash from the projection itself. Neither half can be
 * replaced independently.
 */
@ValueObject
public record SyncSnapshot(SyncProjection projection, SyncHash hash) {

    public SyncSnapshot {
        Assert.notNull(projection, "projection is required");
        Assert.notNull(hash, "hash is required");
    }

    public static SyncSnapshot of(SyncProjection projection, SyncProjectionHasher hasher) {
        Assert.notNull(projection, "projection is required");
        Assert.notNull(hasher, "hasher is required");
        return new SyncSnapshot(projection, hasher.hash(projection));
    }

    /**
     * Reconstructs a snapshot from a projection and a hash already computed and stored
     * elsewhere. Used only by the persistence layer when loading a record back from the
     * database — application code always builds a snapshot from a projection via
     * {@link #of(SyncProjection)}, which is the only way to guarantee the hash matches.
     */
    public static SyncSnapshot reconstruct(SyncProjection projection, SyncHash hash) {
        return new SyncSnapshot(projection, hash);
    }

    public boolean matches(SyncSnapshot other) {
        return other != null && this.hash.equals(other.hash);
    }
}
