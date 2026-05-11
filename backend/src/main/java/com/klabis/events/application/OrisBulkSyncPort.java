package com.klabis.events.application;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface OrisBulkSyncPort {

    /**
     * Synchronises all upcoming ORIS-imported events that are eligible for sync.
     * <p>
     * Eligible events: status IN (DRAFT, ACTIVE), eventDate >= today, orisEventId IS NOT NULL.
     * <p>
     * Each event is processed independently — a failure on one does not stop the rest.
     * Always returns a summary, even when all events fail.
     *
     * @return aggregate result with per-event status entries
     */
    BulkSyncResult syncAllUpcoming();
}
