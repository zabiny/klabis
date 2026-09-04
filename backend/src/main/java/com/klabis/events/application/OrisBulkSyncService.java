package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.oris.OrisIntegrationComponent;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs a manual pass over every active event synchronisation record (design.md D18,
 * task 8.5) instead of looping over upcoming ORIS events and re-importing each one —
 * the engine, not this service, now owns direction resolution and conflict detection.
 * Records already {@code CONFLICT} or {@code FAILED} are not attempted; they are
 * counted and listed separately, since only a decision on their synchronisation
 * resource — not a retry — can move them again.
 */
@Service
@OrisIntegrationComponent
class OrisBulkSyncService implements OrisBulkSyncPort {

    private static final Logger log = LoggerFactory.getLogger(OrisBulkSyncService.class);

    private final EventRepository eventRepository;
    private final SynchronizationPort synchronizationPort;

    OrisBulkSyncService(EventRepository eventRepository, SynchronizationPort synchronizationPort) {
        this.eventRepository = eventRepository;
        this.synchronizationPort = synchronizationPort;
    }

    @Override
    public BulkSyncResult syncAllUpcoming() {
        List<SyncRecord> activeRecords = synchronizationPort.findActiveByEntityType(SyncEntityType.EVENT);

        List<BulkSyncResult.EventSyncEntry> results = new ArrayList<>();
        List<BulkSyncResult.EventSyncEntry> awaitingDecision = new ArrayList<>();
        List<BulkSyncResult.EventSyncEntry> stoppedByFailure = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (SyncRecord record : activeRecords) {
            EventId eventId = toEventId(record);
            String eventName = eventName(eventId);

            if (record.getStatus() == SyncStatus.CONFLICT) {
                awaitingDecision.add(BulkSyncResult.EventSyncEntry.notAttempted(eventId, eventName));
                continue;
            }
            if (record.getStatus() == SyncStatus.FAILED) {
                stoppedByFailure.add(BulkSyncResult.EventSyncEntry.notAttempted(eventId, eventName));
                continue;
            }

            try {
                synchronizationPort.synchronizeNow(record.getId(), null);
                results.add(BulkSyncResult.EventSyncEntry.synced(eventId, eventName));
                successCount++;
            } catch (Exception e) {
                log.warn("Bulk ORIS sync failed for event {} ({}): {}", eventId, eventName, e.getMessage());
                results.add(BulkSyncResult.EventSyncEntry.failed(eventId, eventName, e.getMessage()));
                failureCount++;
            }
        }

        return new BulkSyncResult(
                activeRecords.size(), successCount, failureCount,
                awaitingDecision.size(), stoppedByFailure.size(),
                results, awaitingDecision, stoppedByFailure);
    }

    private EventId toEventId(SyncRecord record) {
        return EventId.of(UUID.fromString(record.getTarget().entityId()));
    }

    private String eventName(EventId eventId) {
        return eventRepository.findById(eventId).map(Event::getName).orElse(null);
    }
}
