package com.klabis.common.sync;

import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

@Service
public class DataSyncImpl implements DataSync {

    private static final Logger log = LoggerFactory.getLogger(DataSyncImpl.class);

    private final Collection<SyncLine<?, ?>> syncLines;

    public DataSyncImpl(Collection<SyncLine<?, ?>> syncLines) {
        this.syncLines = syncLines;
    }

    @Override
    public SyncRecord sync(SyncId syncId, Direction direction) {
        SyncLine<?, ?> syncLine = findSyncLine(syncId)
                .orElseThrow(() -> new IllegalArgumentException("No sync line found for " + syncId));
        boolean push = direction == Direction.PUSH;
        try {
            SyncId other = push ? syncLine.push(syncId) : syncLine.pull(syncId);
            return SyncRecord.success(push ? syncId : other, push ? other : syncId);
        } catch (Exception e) {
            log.warn("Sync failed for {}", syncId, e);
            String cause = e.getMessage() != null ? e.getMessage() : e.toString();
            return SyncRecord.failure(push ? syncId : null, push ? null : syncId, cause);
        }
    }

    private Optional<SyncLine<?, ?>> findSyncLine(SyncId syncId) {
        return syncLines.stream().filter(t -> t.matches(syncId)).findAny();
    }
}
