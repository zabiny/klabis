package com.klabis.events.application;

import com.klabis.events.domain.Event;
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

    private final OrisEventImportPort orisEventImportPort;

    OrisEventBulkImportService(OrisEventImportPort orisEventImportPort) {
        this.orisEventImportPort = orisEventImportPort;
    }

    @Override
    public BulkImportResult importEventsFromOris(List<Integer> orisIds) {
        List<BulkImportResult.EventImportEntry> results = new ArrayList<>();

        for (int orisId : orisIds) {
            try {
                Event imported = orisEventImportPort.importEventFromOris(orisId);
                results.add(BulkImportResult.EventImportEntry.imported(orisId, imported.getName(), imported.getEventDate()));
            } catch (Exception e) {
                log.warn("Bulk ORIS import failed for orisId {}: {}", orisId, e.getMessage());
                results.add(BulkImportResult.EventImportEntry.failed(orisId, null, null, e.getMessage()));
            }
        }

        int successCount = (int) results.stream()
                .filter(r -> r.status() == BulkImportResult.ImportStatus.IMPORTED)
                .count();
        int failureCount = results.size() - successCount;
        return new BulkImportResult(orisIds.size(), successCount, failureCount, results);
    }
}
