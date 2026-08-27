package com.klabis.common.sync;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

class FakeSyncRecordRepository implements SyncRecordRepository {

    private final Collection<SyncRecord> records;

    FakeSyncRecordRepository(SyncRecord... records) {
        this.records = new HashSet<>(Arrays.asList(records));
    }

    @Override
    public Optional<SyncRecord> findById(SyncId id) {
        return records.stream()
                .filter(t -> id.equals(t.localId()) || id.equals(t.externalId()))
                .findAny();
    }

    @Override
    public SyncRecord save(SyncRecord record) {
        records.add(record);
        return record;
    }
}
