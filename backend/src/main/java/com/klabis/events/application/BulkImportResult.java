package com.klabis.events.application;

import java.time.LocalDate;
import java.util.List;

/**
 * Result of a bulk ORIS import operation.
 *
 * @param totalProcessed total number of orisIds submitted for import
 * @param successCount   number of events successfully imported
 * @param failureCount   number of events that failed to import
 * @param results        per-event import results
 */
public record BulkImportResult(
        int totalProcessed,
        int successCount,
        int failureCount,
        List<EventImportEntry> results
) {

    /**
     * Per-event result entry in a bulk import operation.
     *
     * @param orisId ORIS event identifier
     * @param name   event name (from imported event on success, or from ORIS summary if available on failure)
     * @param date   event date (from imported event on success, or from ORIS summary if available on failure)
     * @param status import outcome
     * @param error  error message when status is {@link ImportStatus#FAILED}, null otherwise
     */
    public record EventImportEntry(
            int orisId,
            String name,
            LocalDate date,
            ImportStatus status,
            String error
    ) {

        static EventImportEntry imported(int orisId, String name, LocalDate date) {
            return new EventImportEntry(orisId, name, date, ImportStatus.IMPORTED, null);
        }

        static EventImportEntry failed(int orisId, String name, LocalDate date, String error) {
            return new EventImportEntry(orisId, name, date, ImportStatus.FAILED, error);
        }
    }

    public enum ImportStatus {
        IMPORTED, FAILED
    }
}
