package com.klabis.events.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.sync.DataSync;
import com.klabis.common.sync.SyncRecord;
import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.oris.OrisIntegrationComponent;
import org.springframework.stereotype.Service;

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
                EventSyncIds.externalSyncId(orisId), DataSync.Direction.PULL);
        if (record.result() == DataSync.SyncResult.ERROR) {
            throw rethrow(record.failureException());
        }
        EventId eventId = EventSyncIds.toEventId(record.localId());
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
                EventSyncIds.localSyncId(eventId), DataSync.Direction.PULL);
        if (record.result() == DataSync.SyncResult.ERROR) {
            throw rethrow(record.failureException());
        }
    }

    private static RuntimeException rethrow(Throwable failure) {
        if (failure instanceof RuntimeException re) {
            return re;
        }
        if (failure instanceof Error err) {
            throw err;
        }
        return new IllegalStateException("ORIS sync failed", failure);
    }
}
