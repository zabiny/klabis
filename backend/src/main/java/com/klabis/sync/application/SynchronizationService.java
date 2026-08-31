package com.klabis.sync.application;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Orchestrates one synchronisation pass (design.md D9, "How a pass runs"). Slice 1
 * covers: the version-token short-circuit, reading both sides, comparing against the
 * baseline, adopting the external side on first enrolment, writing inward when only
 * the external side changed, and appending the attempt to history.
 * <p>
 * Outward writes, convergence, conflicts, retry/claim handling and scheduling are
 * added by later slices around this same orchestration.
 */
@Service
class SynchronizationService implements SynchronizationPort {

    private final SyncRecordRepository syncRecordRepository;
    private final SyncAttemptRepository syncAttemptRepository;
    private final SynchronizationAdapterRegistry adapterRegistry;
    private final SyncProjectionHasher hasher;

    SynchronizationService(
            SyncRecordRepository syncRecordRepository,
            SyncAttemptRepository syncAttemptRepository,
            SynchronizationAdapterRegistry adapterRegistry,
            SyncProjectionHasher hasher
    ) {
        this.syncRecordRepository = syncRecordRepository;
        this.syncAttemptRepository = syncAttemptRepository;
        this.adapterRegistry = adapterRegistry;
        this.hasher = hasher;
    }

    @Transactional
    @Override
    public SyncRecord enroll(SyncTarget target, ExternalReference externalReference) {
        adapterRegistry.find(target.entityType(), externalReference.system())
                .orElseThrow(() -> new UnknownSyncEntityTypeException(target.entityType(), externalReference.system()));

        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), target, externalReference);
        return syncRecordRepository.save(record);
    }

    @Transactional
    @Override
    public SyncRecord synchronizeNow(SyncRecordId id, String actingUser) {
        SyncRecord record = getOrThrow(id);
        SynchronizationAdapter adapter = adapterRegistry.find(record.getTarget().entityType(), record.getExternalReference().system())
                .orElseThrow(() -> new UnknownSyncEntityTypeException(record.getTarget().entityType(), record.getExternalReference().system()));

        return runPass(record, adapter, SyncTriggerKind.MANUAL, actingUser);
    }

    @Transactional(readOnly = true)
    @Override
    public SyncRecord state(SyncRecordId id) {
        return getOrThrow(id);
    }

    @Transactional
    @Override
    public void retire(SyncRecordId id) {
        SyncRecord record = getOrThrow(id);
        record.retire();
        syncRecordRepository.save(record);
    }

    private SyncRecord getOrThrow(SyncRecordId id) {
        return syncRecordRepository.findById(id).orElseThrow(() -> new SyncRecordNotFoundException(id));
    }

    /**
     * Runs one pass: version-token short-circuit, reads, comparison, write, and
     * appending the attempt to history (design.md D9, D3).
     */
    private SyncRecord runPass(SyncRecord record, SynchronizationAdapter adapter, SyncTriggerKind trigger, String actingUser) {
        String entityId = record.getTarget().entityId();
        String externalId = record.getExternalReference().externalId();

        if (record.getBaseline() != null && record.getDirtySince() == null) {
            Optional<ExternalVersionToken> currentToken = adapter.externalVersion(externalId);
            if (currentToken.isPresent() && currentToken.get().equals(record.getExternalVersion())) {
                // Cheap change indicator unchanged and no local edit observed: skip the
                // full read entirely (design.md D3). Still recorded as an attempt — D15
                // requires every attempt to appends a row, including this one.
                appendAttempt(record, trigger, null, SyncOutcome.SKIPPED,
                        record.getLocal() != null ? record.getLocal().hash() : null,
                        record.getExternal() != null ? record.getExternal().hash() : null,
                        null, actingUser);
                return record;
            }
        }

        SyncProjection localProjection = adapter.readLocal(entityId);
        SyncProjection externalProjection = adapter.readExternal(externalId);
        SyncSnapshot currentLocal = SyncSnapshot.of(localProjection, hasher);
        SyncSnapshot currentExternal = SyncSnapshot.of(externalProjection, hasher);

        SyncDecision decision = record.decide(currentLocal, currentExternal);

        SyncHash localHashForAttempt = currentLocal.hash();
        SyncHash externalHashForAttempt = currentExternal.hash();

        switch (decision.kind()) {
            case NOTHING_TO_DO -> {
                appendAttempt(record, trigger, null, SyncOutcome.SUCCESS, localHashForAttempt, externalHashForAttempt, null, actingUser);
                return record;
            }
            case ADOPT_EXTERNAL, WRITE -> {
                if (decision.direction() == SyncDirection.INWARD) {
                    writeInward(record, adapter, currentExternal);
                } else {
                    throw new UnsupportedOperationException("Outward writes are added in a later slice");
                }
            }
        }

        adapter.externalVersion(externalId).ifPresent(record::setExternalVersion);
        SyncRecord saved = syncRecordRepository.save(record);
        appendAttempt(saved, trigger, decision.direction(), SyncOutcome.SUCCESS, localHashForAttempt, externalHashForAttempt, null, actingUser);
        return saved;
    }

    /**
     * Writes the external projection to the local side, then re-reads the local
     * projection so the post-write state becomes both the local snapshot and the
     * baseline (design.md D9) — never the pushed value itself, since the write may
     * have been transformed (e.g. field-ownership merges) on the way in.
     */
    private void writeInward(SyncRecord record, SynchronizationAdapter adapter, SyncSnapshot currentExternal) {
        String entityId = record.getTarget().entityId();
        adapter.applyToLocal(entityId, currentExternal.projection());

        SyncProjection postWriteLocal = adapter.readLocal(entityId);
        SyncSnapshot postWriteSnapshot = SyncSnapshot.of(postWriteLocal, hasher);

        record.recordSuccess(SyncDirection.INWARD, postWriteSnapshot, currentExternal);
    }

    private void appendAttempt(
            SyncRecord record,
            SyncTriggerKind trigger,
            SyncDirection direction,
            SyncOutcome outcome,
            SyncHash localHash,
            SyncHash externalHash,
            String failureReason,
            String actingUser
    ) {
        // Scheduled and event-triggered attempts carry no acting user (design.md D15);
        // only manually triggered work does, and it is passed in by the caller.
        String recordedActingUser = trigger == SyncTriggerKind.MANUAL ? actingUser : null;
        syncAttemptRepository.save(SyncAttempt.record(
                record.getId(), trigger, direction, outcome, localHash, externalHash, failureReason, recordedActingUser));
    }
}
