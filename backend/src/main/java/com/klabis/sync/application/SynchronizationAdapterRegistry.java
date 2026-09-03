package com.klabis.sync.application;

import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjection;
import com.klabis.sync.domain.SyncProjectionType;
import com.klabis.sync.domain.SynchronizationAdapter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves the {@link SynchronizationAdapter} registered for a given entity type and
 * external system. Assembled from every {@code SynchronizationAdapter} bean Spring
 * finds — one integration module contributes one adapter bean per entity type it
 * supports.
 * <p>
 * Also implements {@link SyncProjectionType}: an adapter's declared
 * {@link SynchronizationAdapter#projectionType()} is what the persistence layer needs
 * to deserialise a stored projection, so the registry that already knows every
 * adapter is the natural place to answer that question too.
 */
@Component
class SynchronizationAdapterRegistry implements SyncProjectionType {

    private final Map<Key, SynchronizationAdapter> adaptersByKey;

    SynchronizationAdapterRegistry(List<SynchronizationAdapter> adapters) {
        this.adaptersByKey = adapters.stream()
                .collect(java.util.stream.Collectors.toMap(
                        a -> new Key(a.entityType(), a.system()),
                        a -> a));
    }

    Optional<SynchronizationAdapter> find(SyncEntityType entityType, ExternalSystem system) {
        return Optional.ofNullable(adaptersByKey.get(new Key(entityType, system)));
    }

    /**
     * Every external system with a registered adapter for {@code entityType} — used to
     * resolve a REST call addressed by entity type alone (no external system in the
     * URL, design.md D14) down to the one record it can mean. Ordinarily exactly one;
     * see {@link SynchronizationPort#findByTarget} for how the caller handles zero or
     * more than one.
     */
    List<ExternalSystem> systemsFor(SyncEntityType entityType) {
        return adaptersByKey.keySet().stream()
                .filter(key -> key.entityType() == entityType)
                .map(Key::system)
                .toList();
    }

    @Override
    public Class<? extends SyncProjection> classFor(SyncEntityType entityType) {
        return adaptersByKey.values().stream()
                .filter(adapter -> adapter.entityType() == entityType)
                .map(SynchronizationAdapter::projectionType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No synchronisation adapter registered for entity type " + entityType));
    }

    private record Key(SyncEntityType entityType, ExternalSystem system) {
    }
}
