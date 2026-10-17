package com.klabis.sync.application;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.SyncStatus;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

/**
 * Read-only view over the synchronisation history across all records. Kept apart from the
 * repositories because the reads join attempts with their record and filter in SQL.
 */
@PrimaryPort
public interface SyncHistoryQuery {

    /**
     * Newest first. The sort of {@code pageable} is ignored.
     */
    Page<AttemptView> listAttempts(AttemptFilter filter, Pageable pageable);

    /**
     * Attempts of one record, newest first, applying the same "changed nothing" rule as
     * {@link #listAttempts}. The sort of {@code pageable} is ignored.
     */
    Page<AttemptView> listAttemptsOf(SyncRecordId recordId, boolean includeUnchanged, Pageable pageable);

    /**
     * Records currently in any of {@code statuses}; empty for an empty set.
     */
    List<RecordView> listRecordsIn(Set<SyncStatus> statuses);
}
