package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.domain.*;
import com.klabis.common.OrisIntegrationComponent;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.application.SyncRecordNeedsResolutionException;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncStatus;
import com.klabis.sync.domain.SyncTarget;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@OrisIntegrationComponent
class OrisEventImportService implements OrisEventImportPort {

    private final EventRepository eventRepository;
    private final SynchronizationPort synchronizationPort;

    OrisEventImportService(EventRepository eventRepository,
                           SynchronizationPort synchronizationPort) {
        this.eventRepository = eventRepository;
        this.synchronizationPort = synchronizationPort;
    }

    /**
     * Delegates to the synchronisation engine's {@code pullAndEnroll} (design.md D10,
     * task 9.2) instead of building the event and enrolling it by hand: creation,
     * pairing and the initial pass all now live in the engine, with
     * {@code OrisEventSyncAdapter.createLocal} supplying the ORIS-specific build
     * (design.md D2, D3). A repeated import synchronises the existing pairing and
     * returns the existing event rather than failing with a duplicate (design.md D5) —
     * the old {@code DuplicateOrisImportException} is unreachable from this path and
     * has been removed (design.md Open Questions, task 9.8).
     * <p>
     * A pairing already awaiting a decision (CONFLICT or FAILED) is refused by the
     * engine with {@link SyncRecordNeedsResolutionException}, translated here into
     * {@link EventSyncNeedsResolutionException} for the same reason
     * {@link #syncEventFromOris} does: this module's REST layer should not need to
     * know the sync module's internal exception vocabulary.
     */
    @Transactional
    @Override
    public Event importEventFromOris(int orisId) {
        SyncRecord record;
        try {
            record = synchronizationPort.pullAndEnroll(
                    SyncEntityType.EVENT,
                    new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisId)),
                    null);
        } catch (SyncRecordNeedsResolutionException e) {
            throw new EventSyncNeedsResolutionException(orisId);
        }

        EventId eventId = new EventId(UUID.fromString(record.getTarget().entityId()));
        return eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
    }

    /**
     * Delegates to the synchronisation engine (design.md D18, task 8.3) instead of
     * overwriting the event's ORIS-owned fields itself: a local edit to one of them
     * now surfaces as a conflict rather than being silently discarded (design.md D6 —
     * the behaviour change task 8.9 covers in the pre-existing tests that assumed the
     * old silent-overwrite semantics).
     * <p>
     * Refuses up front, without claiming or attempting the record, when it is already
     * {@code CONFLICT} or {@code FAILED} — the same guard
     * {@link SynchronizationPort#synchronizeNow} applies, surfaced here as
     * {@link EventSyncNeedsResolutionException} instead of the sync module's own
     * exception type so this module's REST layer needs no knowledge of sync's
     * internal exception vocabulary.
     */
    @Override
    public void syncEventFromOris(EventId eventId) {
        eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));

        SyncTarget target = new SyncTarget(SyncEntityType.EVENT, eventId.value().toString());
        SyncRecord record = synchronizationPort.findByTarget(target)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (record.getStatus() == SyncStatus.CONFLICT || record.getStatus() == SyncStatus.FAILED) {
            throw new EventSyncNeedsResolutionException(eventId);
        }

        synchronizationPort.synchronizeNow(record.getId(), null);
    }
}
