package com.klabis.events.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.sync.DataSync;
import com.klabis.common.sync.SyncId;
import com.klabis.common.sync.SyncRecord;
import com.klabis.common.sync.SyncType;
import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.oris.OrisIntegrationComponent;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@OrisIntegrationComponent
class OrisEventImportService implements OrisEventImportPort {

    private final DataSync dataSync;
    private final EventRepository eventRepository;

    OrisEventImportService(DataSync dataSync, EventRepository eventRepository) {
        this.dataSync = dataSync;
        this.eventRepository = eventRepository;
    }

    @Override
    public Event importEventFromOris(int orisId) {
        SyncRecord record = dataSync.sync(
                SyncId.externalId(SyncType.EVENT, Integer.toString(orisId)), DataSync.Direction.PULL);
        if (record.result() == DataSync.SyncResult.ERROR) {
            throw translate(orisId, record.failureCause());
        }
        EventId eventId = eventIdOf(record.localId());
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(orisId));
    }

    @Override
    public void syncEventFromOris(EventId eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (event.getOrisId() == null) {
            throw new BusinessRuleViolationException(
                    "Event %s has no orisId, cannot sync from ORIS".formatted(eventId)) {
            };
        }
        SyncRecord record = dataSync.sync(
                SyncId.localId(SyncType.EVENT, eventId.value().toString()), DataSync.Direction.PULL);
        if (record.result() == DataSync.SyncResult.ERROR) {
            throw translate(event.getOrisId(), record.failureCause());
        }
    }

    private static EventId eventIdOf(SyncId localId) {
        return new EventId(UUID.fromString(localId.idValue()));
    }

    /**
     * Fragile shim: maps substrings of {@link SyncRecord#failureCause()} back onto the typed
     * exceptions the REST layer's exception handlers expect, so the HTTP status codes of the
     * single-event ORIS endpoints stay unchanged after the switch to {@link DataSync}. The
     * substrings are the exception messages produced by {@code DataSyncImpl}, {@code SyncLine},
     * {@code OrisEventMappingSupport.buildRegistrationDeadlines} and the JDBC layer. A cleaner
     * follow-up (result body with status, like the bulk endpoints) is tracked in the plan.
     */
    private static RuntimeException translate(int orisId, String failureCause) {
        String cause = failureCause == null ? "" : failureCause.toLowerCase(Locale.ROOT);
        if (cause.contains("constraint") || cause.contains("duplicate") || cause.contains("unique")) {
            return new DuplicateOrisImportException(orisId);
        }
        if (cause.contains("not found") || cause.contains("no sync record") || cause.contains("no sync line")) {
            return new EventNotFoundException(orisId);
        }
        if (cause.contains("registration deadline") || cause.contains("invalid")) {
            return new BusinessRuleViolationException(failureCause) {
            };
        }
        return new BusinessRuleViolationException("ORIS sync failed: " + failureCause) {
        };
    }
}
