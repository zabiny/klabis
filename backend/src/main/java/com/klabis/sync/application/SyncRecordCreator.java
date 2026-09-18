package com.klabis.sync.application;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ScheduleEffect;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncRecordRepository;
import com.klabis.sync.domain.SyncScheduleRepository;
import com.klabis.sync.domain.SyncTarget;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists a newly created local entity's pairing (design.md D8): the record save and
 * its schedule row commit together, in one short transaction. Used directly by
 * {@link SynchronizationService#enroll}, and by {@link SynchronizationPort#pullAndEnroll}'s
 * "no pairing" branch as a separate bean — that branch calls this from the same class
 * as the external read that must precede it with no transaction open, and only a
 * genuine cross-bean call goes through the Spring AOP proxy that applies
 * {@code @Transactional} (see {@link SyncRecordClaimer}'s javadoc for the same
 * reasoning).
 */
@Service
class SyncRecordCreator {

    private final SyncRecordRepository syncRecordRepository;
    private final SyncScheduleRepository syncScheduleRepository;

    SyncRecordCreator(SyncRecordRepository syncRecordRepository, SyncScheduleRepository syncScheduleRepository) {
        this.syncRecordRepository = syncRecordRepository;
        this.syncScheduleRepository = syncScheduleRepository;
    }

    @Transactional
    SyncRecord createAndPair(SyncTarget target, ExternalReference externalReference) {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), target, externalReference);
        SyncRecord saved = syncRecordRepository.save(record);
        syncScheduleRepository.createFor(saved.getId());
        return saved;
    }

    /**
     * Persists a reactivated record (design.md D6, D7, "Domain Changes"): the record
     * save and its {@link com.klabis.sync.domain.ScheduleEffect} commit together, so
     * the pairing is due again the instant reactivation is visible.
     */
    @Transactional
    SyncRecord persistReactivation(SyncRecord record, ScheduleEffect scheduleEffect) {
        SyncRecord saved = syncRecordRepository.save(record);
        syncScheduleRepository.apply(saved.getId(), scheduleEffect);
        saved.updateSchedule(record.getSchedule());
        return saved;
    }
}
