package com.klabis.sync.application;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
}
