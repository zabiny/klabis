package com.klabis.events.application;

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
    private final OrisEventImportPort orisEventImportPort;

    OrisBulkSyncService(EventRepository eventRepository, OrisEventImportPort orisEventImportPort) {
        this.eventRepository = eventRepository;
        this.orisEventImportPort = orisEventImportPort;
    }

    @Override
    public BulkSyncResult syncAllUpcoming() {
        List<Event> events = eventRepository.findAllUpcomingOrisEvents(LocalDate.now());

        List<BulkSyncResult.EventSyncEntry> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (Event event : events) {
            try {
                orisEventImportPort.syncEventFromOris(event.getId());
                results.add(BulkSyncResult.EventSyncEntry.synced(event.getId(), event.getName()));
                successCount++;
            } catch (Exception e) {
                log.warn("Bulk ORIS sync failed for event {} ({}): {}", event.getId(), event.getName(), e.getMessage());
                results.add(BulkSyncResult.EventSyncEntry.failed(event.getId(), event.getName(), e.getMessage()));
                failureCount++;
            }
        }

        return new BulkSyncResult(events.size(), successCount, failureCount, results);
    }
}
