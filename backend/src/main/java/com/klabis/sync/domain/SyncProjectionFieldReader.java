package com.klabis.sync.domain;

import java.util.Map;

/**
 * Breaks a {@link SyncProjection} into its named fields for per-field divergence
 * attribution (design.md D14 response shape). Computed by comparing decrypted
 * projections in memory — never from stored per-field hashes (design.md D13), which
 * is exactly why this reads a live, already-decrypted {@link SyncProjection} rather
 * than anything persisted.
 * <p>
 * The domain depends only on this port; turning a projection into named fields is an
 * infrastructure concern (uses Jackson) and lives behind
 * {@code com.klabis.sync.infrastructure.SyncProjectionCodec}, mirroring
 * {@link SyncProjectionHasher}.
 */
public interface SyncProjectionFieldReader {

    Map<String, Object> fields(SyncProjection projection);
}
