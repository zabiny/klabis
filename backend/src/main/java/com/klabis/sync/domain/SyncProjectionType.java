package com.klabis.sync.domain;

/**
 * Resolves the concrete {@link SyncProjection} class for a {@link SyncEntityType}, so
 * the persistence layer can deserialise stored JSON back into the right type without
 * the {@code sync} module knowing any concrete projection shape itself.
 * <p>
 * Implemented by a Spring bean assembled from every registered
 * {@link SynchronizationAdapter} — each adapter's projection type is registered for
 * its {@link SynchronizationAdapter#entityType()}.
 */
public interface SyncProjectionType {

    Class<? extends SyncProjection> classFor(SyncEntityType entityType);
}
