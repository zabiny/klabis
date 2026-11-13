package com.klabis.sync.fixtures;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncRecordRepository;
import com.klabis.sync.domain.SyncScheduleRepository;
import com.klabis.sync.domain.SyncTarget;
import org.springframework.boot.test.context.TestComponent;

/**
 * Creates a paired, scheduled {@link SyncRecord} directly through the repositories,
 * bypassing the engine, for tests that need an existing pairing as a precondition.
 */
@TestComponent
public class PairedSyncRecordFixture {

    private final SyncRecordRepository syncRecordRepository;
    private final SyncScheduleRepository syncScheduleRepository;

    PairedSyncRecordFixture(SyncRecordRepository syncRecordRepository, SyncScheduleRepository syncScheduleRepository) {
        this.syncRecordRepository = syncRecordRepository;
        this.syncScheduleRepository = syncScheduleRepository;
    }

    public SyncRecord pair(SyncTarget target, ExternalReference externalReference) {
        SyncRecord saved = syncRecordRepository.save(SyncRecord.enroll(SyncRecordId.newId(), target, externalReference));
        syncScheduleRepository.createFor(saved.getId());
        return saved;
    }
}
