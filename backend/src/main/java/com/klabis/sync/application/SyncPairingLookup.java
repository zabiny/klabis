package com.klabis.sync.application;

import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncedEntityReference;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Collection;
import java.util.List;

/**
 * Read-only lookups of what is paired with what. Kept apart from {@link SynchronizationPort} so that an
 * adapter can resolve pairings without depending on the engine, which itself depends on every adapter
 * (through {@code SynchronizationAdapterRegistry}) and would close a bean cycle.
 */
@PrimaryPort
public interface SyncPairingLookup {

    /**
     * Which of these external ids already have any record, {@code RETIRED} included, for this entity type and system.
     */
    List<SyncedEntityReference> findByExternalReferences(SyncEntityType entityType, ExternalSystem system,
                                                         Collection<String> externalIds);

    /**
     * Which of these entities are actively enrolled for synchronisation.
     */
    List<SyncedEntityReference> findActiveByTargets(SyncEntityType entityType, Collection<String> entityIds);

    /**
     * Every non-retired record of the entity type, {@code FAILED} included.
     */
    List<SyncRecord> findActiveByEntityType(SyncEntityType entityType);
}
