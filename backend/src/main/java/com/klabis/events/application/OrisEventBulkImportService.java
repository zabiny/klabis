package com.klabis.events.application;

import com.klabis.common.sync.DataSync;
import com.klabis.common.sync.SyncRecord;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.oris.OrisIntegrationComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@OrisIntegrationComponent
class OrisEventBulkImportService implements OrisEventBulkImportPort {

    private static final Logger log = LoggerFactory.getLogger(OrisEventBulkImportService.class);

    private final DataSync dataSync;
    private final EventRepository eventRepository;

    OrisEventBulkImportService(DataSync dataSync, EventRepository eventRepository) {
        this.dataSync = dataSync;
        this.eventRepository = eventRepository;
    }

    @Override
    public BulkImportResult importEventsFromOris(List<Integer> orisIds) {
        List<BulkImportResult.EventImportEntry> results = new ArrayList<>();

        for (int orisId : orisIds) {
            SyncRecord record = dataSync.sync(
                    EventSyncIds.externalSyncId(orisId), DataSync.Direction.PULL);
            if (record.result() == DataSync.SyncResult.SYNCED) {
                Event imported = eventRepository.findById(EventSyncIds.toEventId(record.localId()))
                        .orElseThrow(() -> new IllegalStateException(
                                "ORIS event %d reported SYNCED but not found in repository".formatted(orisId)));
                results.add(BulkImportResult.EventImportEntry.imported(
                        orisId, imported.getName(), imported.getEventDate()));
            } else {
                String error = failureMessage(record.failureException());
                log.warn("Bulk ORIS import failed for orisId {}: {}", orisId, error);
                results.add(BulkImportResult.EventImportEntry.failed(orisId, null, null, error));
            }
        }

        int successCount = (int) results.stream()
                .filter(r -> r.status() == BulkImportResult.ImportStatus.IMPORTED)
                .count();
        int failureCount = results.size() - successCount;
        return new BulkImportResult(orisIds.size(), successCount, failureCount, results);
    }

    private static String failureMessage(Throwable t) {
        return t.getMessage() != null ? t.getMessage() : t.toString();
    }
}
