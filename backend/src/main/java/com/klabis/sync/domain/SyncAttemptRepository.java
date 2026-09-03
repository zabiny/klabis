package com.klabis.sync.domain;

import com.klabis.sync.SyncRecordId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.time.Instant;
import java.util.List;

/**
 * Append-only history of {@link SyncAttempt} rows (design.md D15). The only deletion
 * is retention pruning (design.md D19) — it never touches {@code sync_record} itself.
 */
@SecondaryPort
public interface SyncAttemptRepository {

    SyncAttempt save(SyncAttempt attempt);

    /**
     * Most recent attempts for a record, newest first — backs the derived failure
     * count (design.md D10).
     */
    List<SyncAttempt> findByRecordIdOrderByStartedAtDesc(SyncRecordId recordId);

    /**
     * Deletes attempt rows started before {@code olderThan} (design.md D19's
     * {@code history-retention}). Never deletes {@code sync_record} rows — the last
     * successful sync timestamp lives on the record itself, so pruning history loses
     * no information needed to keep syncing.
     *
     * @return the number of rows deleted
     */
    int deleteAttemptsStartedBefore(Instant olderThan);
}
