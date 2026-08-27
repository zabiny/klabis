package com.klabis.events.application;

import com.klabis.common.sync.DataSync;
import com.klabis.common.sync.SyncId;
import com.klabis.common.sync.SyncRecord;
import com.klabis.common.sync.SyncType;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.oris.OrisIntegrationComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@OrisIntegrationComponent
class OrisBulkSyncService implements OrisBulkSyncPort {

    private static final Logger log = LoggerFactory.getLogger(OrisBulkSyncService.class);

    private final EventRepository eventRepository;
    private final DataSync dataSync;

    OrisBulkSyncService(EventRepository eventRepository, DataSync dataSync) {
        this.eventRepository = eventRepository;
        this.dataSync = dataSync;
    }

    @Override
    public BulkSyncResult syncAllUpcoming() {
        List<Event> events = eventRepository.findAllUpcomingOrisEvents(LocalDate.now());

        List<BulkSyncResult.EventSyncEntry> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (Event event : events) {
            SyncRecord record = dataSync.sync(
                    SyncId.localId(SyncType.EVENT, event.getId().value().toString()),
                    DataSync.Direction.PULL);
            if (record.result() == DataSync.SyncResult.SYNCED) {
                results.add(BulkSyncResult.EventSyncEntry.synced(event.getId(), event.getName()));
                successCount++;
            } else {
                String error = failureMessage(record.failureException());
                log.warn("Bulk ORIS sync failed for event {} ({}): {}",
                        event.getId(), event.getName(), error);
                results.add(BulkSyncResult.EventSyncEntry.failed(
                        event.getId(), event.getName(), error));
                failureCount++;
            }
        }

        return new BulkSyncResult(events.size(), successCount, failureCount, results);
    }

    private static String failureMessage(Throwable t) {
        return t.getMessage() != null ? t.getMessage() : t.toString();
    }
}
