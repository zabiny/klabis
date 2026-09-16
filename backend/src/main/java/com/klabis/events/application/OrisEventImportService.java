package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.domain.*;
import com.klabis.common.OrisIntegrationComponent;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncStatus;
import com.klabis.sync.domain.SyncTarget;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@OrisIntegrationComponent
class OrisEventImportService implements OrisEventImportPort {

    private final EventRepository eventRepository;
    private final OrisEventFieldsGateway orisEventFieldsGateway;
    private final SynchronizationPort synchronizationPort;

    OrisEventImportService(EventRepository eventRepository,
                           OrisEventFieldsGateway orisEventFieldsGateway,
                           SynchronizationPort synchronizationPort) {
        this.eventRepository = eventRepository;
        this.orisEventFieldsGateway = orisEventFieldsGateway;
        this.synchronizationPort = synchronizationPort;
    }

    @Transactional
    @Override
    public Event importEventFromOris(int orisId) {
        OrisEventFields fields = orisEventFieldsGateway.readOrisFields(orisId);

        Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(orisId)
                .name(fields.name())
                .eventDate(fields.eventDate())
                .location(fields.location())
                .organizer(fields.organizer())
                .websiteUrl(fields.websiteUrl())
                .registrationDeadlines(fields.registrationDeadlines())
                .categories(fields.categories())
                .ranking(fields.ranking())
                .baseEntryFee(fields.baseEntryFee())
                .build());

        event.applyAutoMappedEventType(fields.resolvedEventTypeId());

        Event saved;
        try {
            saved = eventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateOrisImportException(orisId);
        }

        // Enrolment happens synchronously here, colocated with the ORIS-specific import
        // path, rather than via a listener on EventCreatedEvent — that event fires for
        // every event, manually created ones included, so a listener would need an
        // orisId != null filter this path never needs (design.md D17, task 8.1).
        synchronizationPort.enroll(
                new SyncTarget(SyncEntityType.EVENT, saved.getId().value().toString()),
                new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisId)));

        return saved;
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
