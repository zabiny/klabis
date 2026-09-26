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
import org.springframework.stereotype.Service;

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
     * {@link EventSyncNeedsResolutionException} so this module's REST layer needs no
     * knowledge of the sync module's internal exception vocabulary.
     * <p>
     * Deliberately NOT {@code @Transactional}: {@code pullAndEnroll} performs a
     * blocking external ORIS HTTP call and relies on {@code SyncRecordCreator} and the
     * engine's claim/pass steps being genuine cross-bean calls with their own short
     * transactions (design.md D8). Wrapping this method in a transaction would keep it
     * open across the external call, defeating that design — see
     * {@link com.klabis.sync.application.SynchronizationService} javadoc.
     */
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
}
