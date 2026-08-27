package com.klabis.common.sync;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dummy in-memory {@link SyncRecordRepository}. Placeholder until a persistent (JDBC) adapter is
 * introduced. Records are keyed by their surrogate {@code id}; lookups match on either
 * {@code localId} or {@code externalId}.
 */
@SecondaryAdapter
@Repository
class InMemorySyncRecordRepository implements SyncRecordRepository {

    private final Map<UUID, SyncRecord> records = new ConcurrentHashMap<>();

    @Override
    public Optional<SyncRecord> findById(SyncId id) {
        return records.values().stream()
                .filter(r -> id.equals(r.localId()) || id.equals(r.externalId()))
                .findAny();
    }

    @Override
    public SyncRecord save(SyncRecord record) {
        records.put(record.id(), record);
        return record;
    }
}
