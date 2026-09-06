package com.klabis.sync.application;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncRecordRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Phase 1 of a pass (design.md D12): read the record and claim it, in one short
 * transaction, before any external call is made. A record whose claim is still fresh
 * is skipped by a concurrent pass ({@link SyncRecordClaimedException}), so a manual
 * trigger and the scheduler never act on the same record at once, while other records
 * remain available to whichever pass gets to them first.
 * <p>
 * A separate bean from {@link SynchronizationService} so the call from
 * {@link SynchronizationService#synchronizeNow} is a genuine cross-bean call — only
 * that goes through the Spring AOP proxy that applies {@code @Transactional}.
 */
@Service
class SyncRecordClaimer {

    private final SyncRecordRepository syncRecordRepository;
    private final SyncProperties properties;
    private final Clock clock;

    SyncRecordClaimer(SyncRecordRepository syncRecordRepository, SyncProperties properties, Clock clock) {
        this.syncRecordRepository = syncRecordRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    SyncRecord claim(SyncRecordId id) {
        SyncRecord record = syncRecordRepository.findById(id).orElseThrow(() -> new SyncRecordNotFoundException(id));
        Instant now = clock.instant();
        if (!record.isClaimAvailable(now, properties.getClaimLease())) {
            throw new SyncRecordClaimedException(id);
        }
        record.claim(now);
        return syncRecordRepository.save(record);
    }
}
