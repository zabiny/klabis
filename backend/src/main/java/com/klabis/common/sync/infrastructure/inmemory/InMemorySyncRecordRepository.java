package com.klabis.common.sync.infrastructure.inmemory;

import com.klabis.common.sync.SyncItemId;
import com.klabis.common.sync.SyncRecord;
import com.klabis.common.sync.SyncRecordRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(InMemorySyncRecordRepository.class);

    @Override
    public Optional<SyncRecord> findById(SyncItemId id) {
        return records.values().stream()
                .filter(r -> id.equals(r.localId()) || id.equals(r.externalId()))
                .findAny();
    }

    @Override
    public SyncRecord save(SyncRecord record) {
        records.put(record.id(), record);
        LOG.info("Synchronization result: {}", describeSyncRecord(record));
        return record;
    }

    private String describeSyncRecord(SyncRecord record) {
        return String.format("SyncRecord[id=%s, localId=%s, externalId=%s, result=%s]",
                record.id(), record.localId(), record.externalId(), record.result());
    }
}
