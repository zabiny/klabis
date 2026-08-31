package com.klabis.sync.domain;

import com.klabis.sync.SyncRecordId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;

/**
 * Append-only history of {@link SyncAttempt} rows (design.md D15). No update or
 * delete beyond retention pruning, added in a later slice.
 */
@SecondaryPort
public interface SyncAttemptRepository {

    SyncAttempt save(SyncAttempt attempt);

    /**
     * Most recent attempts for a record, newest first — backs the derived failure
     * count (design.md D10) added in a later slice.
     */
    List<SyncAttempt> findByRecordIdOrderByStartedAtDesc(SyncRecordId recordId);
}
