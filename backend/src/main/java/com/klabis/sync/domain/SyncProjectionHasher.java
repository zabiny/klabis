package com.klabis.sync.domain;

/**
 * Hashes a {@link SyncProjection} over its whole canonical serialisation
 * (design.md D13) — never per field. The domain depends only on this port; the
 * canonical JSON serialisation itself is an infrastructure concern (uses Jackson) and
 * lives behind {@link com.klabis.sync.infrastructure.SyncProjectionCodec}.
 */
public interface SyncProjectionHasher {

    SyncHash hash(SyncProjection projection);
}
