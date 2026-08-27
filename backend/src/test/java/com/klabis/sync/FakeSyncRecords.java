package com.klabis.sync;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

class FakeSyncRecords implements SyncRecords {

    private final Collection<SyncRecord> records;

    public FakeSyncRecords(Collection<SyncRecord> records) {
        this.records = new HashSet<>(records);
    }

    public FakeSyncRecords(SyncRecord... records) {
        this(Arrays.stream(records).toList());
    }

    @Override
    public Optional<SyncRecord> findById(SyncId id) {
        return records.stream().filter(t -> t.localId().equals(id) || t.externalId().equals(id)).findAny();
    }
}
