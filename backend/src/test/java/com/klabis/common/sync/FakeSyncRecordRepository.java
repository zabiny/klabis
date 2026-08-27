package com.klabis.common.sync;

import java.util.*;

class FakeSyncRecordRepository implements SyncRecordRepository {

    private final Map<UUID, SyncRecord> records = new LinkedHashMap<>();

    FakeSyncRecordRepository(SyncRecord... records) {
        Arrays.asList(records).forEach(r -> this.records.put(r.id(), r));
    }

    @Override
    public Optional<SyncRecord> findById(SyncId id) {
        return records.values().stream()
                .filter(t -> id.equals(t.localId()) || id.equals(t.externalId()))
                .findAny();
    }

    @Override
    public SyncRecord save(SyncRecord record) {
        records.put(record.id(), record);
        return record;
    }

    int size() {
        return records.size();
    }
}
