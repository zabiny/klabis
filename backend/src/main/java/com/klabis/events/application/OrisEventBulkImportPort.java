package com.klabis.events.application;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface OrisEventBulkImportPort {

    /**
     * Imports multiple ORIS events in a single operation.
     * <p>
     * Each event is processed independently via its own transaction — a failure on one does
     * not prevent the remaining events from being imported. Always returns a summary, even
     * when all events fail.
     *
     * @param orisIds list of ORIS event identifiers to import
     * @return aggregate result with per-event status entries
     */
    BulkImportResult importEventsFromOris(List<Integer> orisIds);
}
