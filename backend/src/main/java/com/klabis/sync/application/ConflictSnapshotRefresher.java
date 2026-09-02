package com.klabis.sync.application;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.SyncRecordRepository;
import com.klabis.sync.domain.SyncSnapshot;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refreshes a standing conflict's snapshots from a fresh read when a
 * {@link SynchronizationService#resolveConflict} call is rejected because a side
 * moved since the acknowledgement (design.md D7). A separate bean, not a private
 * method on {@code SynchronizationService}: it must commit in its own transaction
 * ({@code REQUIRES_NEW}) even though the caller goes on to throw, so the refreshed
 * snapshots are not rolled back along with the rejected call — and only a genuine
 * cross-bean call goes through the Spring AOP proxy that applies that propagation.
 */
@Service
class ConflictSnapshotRefresher {

    private final SyncRecordRepository syncRecordRepository;

    ConflictSnapshotRefresher(SyncRecordRepository syncRecordRepository) {
        this.syncRecordRepository = syncRecordRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void refresh(SyncRecordId id, SyncSnapshot freshLocal, SyncSnapshot freshExternal) {
        var record = syncRecordRepository.findById(id).orElseThrow(() -> new SyncRecordNotFoundException(id));
        record.recordConflict(freshLocal, freshExternal, null);
        syncRecordRepository.save(record);
    }
}
