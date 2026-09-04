package com.klabis.events.application;

import com.klabis.events.EventId;

import java.util.List;

/**
 * Result of a manual pass over every active event synchronisation record (design.md
 * D18, tasks 8.4/8.5). {@code totalProcessed} counts every active record considered,
 * including the ones awaiting a decision or stopped by failure; {@code successCount}
 * and {@code failureCount} cover only records actually attempted this pass — a record
 * already {@code CONFLICT} or {@code FAILED} is reported separately, never lumped into
 * {@code failureCount}, since only a decision on its synchronisation resource can move
 * it again.
 * <p>
 * The four counts are derived from the four lists rather than stored independently,
 * so there is exactly one place — the list a caller adds an entry to — that decides
 * what gets counted.
 *
 * @param totalProcessed   total number of active records considered
 * @param results          per-event results for records actually attempted this pass
 * @param awaitingDecision per-event entries for records already in {@code CONFLICT}
 * @param stoppedByFailure per-event entries for records already terminally {@code FAILED}
 */
public record BulkSyncResult(
        int totalProcessed,
        List<EventSyncEntry> results,
        List<EventSyncEntry> awaitingDecision,
        List<EventSyncEntry> stoppedByFailure
) {

    /** Number of records synchronised without incident this pass. */
    public int successCount() {
        return (int) results.stream().filter(entry -> entry.status() == SyncStatus.SYNCED).count();
    }

    /** Number of records that failed this pass (transient or newly terminal). */
    public int failureCount() {
        return (int) results.stream().filter(entry -> entry.status() == SyncStatus.FAILED).count();
    }

    /** Number of records already in {@code CONFLICT} — not attempted. */
    public int awaitingDecisionCount() {
        return awaitingDecision.size();
    }

    /** Number of records already terminally {@code FAILED} — not attempted. */
    public int stoppedByFailureCount() {
        return stoppedByFailure.size();
    }

    /**
     * Per-event result entry in a bulk sync operation.
     * <p>
     * Also used, with {@code status} unset, for entries in {@link #awaitingDecision}
     * and {@link #stoppedByFailure} — those records were not attempted this pass, so
     * neither {@link SyncStatus#SYNCED} nor {@link SyncStatus#FAILED} describes them;
     * which list an entry appears in already says which of the two it is.
     *
     * @param eventId event identifier
     * @param name    event name (snapshot at the time of sync attempt)
     * @param status  sync outcome; null for an entry in {@link #awaitingDecision} or {@link #stoppedByFailure}
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

        static EventSyncEntry notAttempted(EventId eventId, String name) {
            return new EventSyncEntry(eventId, name, null, null);
        }
    }

    public enum SyncStatus {
        SYNCED, FAILED
    }
}
