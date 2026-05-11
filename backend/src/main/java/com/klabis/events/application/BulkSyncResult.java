package com.klabis.events.application;

import com.klabis.events.EventId;

import java.util.List;

/**
 * Result of a bulk ORIS sync operation.
 *
 * @param totalProcessed total number of events that matched the selection criteria
 * @param successCount   number of events successfully synced
 * @param failureCount   number of events that failed to sync
 * @param results        per-event sync results
 */
public record BulkSyncResult(
        int totalProcessed,
        int successCount,
        int failureCount,
        List<EventSyncEntry> results
) {

    /**
     * Per-event result entry in a bulk sync operation.
     *
     * @param eventId event identifier
     * @param name    event name (snapshot at the time of sync attempt)
     * @param status  sync outcome
     * @param error   error message when status is {@link SyncStatus#FAILED}, null otherwise
     */
    public record EventSyncEntry(
            EventId eventId,
            String name,
            SyncStatus status,
            String error
    ) {

        static EventSyncEntry synced(EventId eventId, String name) {
            return new EventSyncEntry(eventId, name, SyncStatus.SYNCED, null);
        }

        static EventSyncEntry failed(EventId eventId, String name, String error) {
            return new EventSyncEntry(eventId, name, SyncStatus.FAILED, error);
        }
    }

    public enum SyncStatus {
        SYNCED, FAILED
    }
}
