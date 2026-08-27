package com.klabis.sync;

import org.jmolecules.ddd.annotation.Service;

import java.util.Collection;
import java.util.Optional;

// consider use https://github.com/FranzDeschler/Synchronization

@Service
public class DataSyncImpl implements DataSync {
    private final Collection<SyncLine<?, ?>> syncSources;
    private final SyncRecords syncRecords;

    public DataSyncImpl(Collection<SyncLine<?, ?>> syncSources, SyncRecords syncRecords) {
        this.syncSources = syncSources;
        this.syncRecords = syncRecords;
    }

    @Override
    public SyncRecord sync(SyncId syncId) throws DataSyncException {
        if (syncId.isLocalId()) {
            return push(syncId);
        } else {
            return pull(syncId);
        }
    }

    public SyncRecord pull(SyncId externalId) {
        SyncLine<?, ?> syncLine = findSyncLine(externalId)
                .orElseThrow(() -> new IllegalArgumentException("No sync line found for " + externalId));

        try {
            SyncId localId = syncLine.pull(externalId);

            return SyncRecord.success(localId, externalId);
        } catch (Exception e) {
            return SyncRecord.failure(null, externalId);
        }
    }

    public SyncRecord push(SyncId localId) {
        SyncLine<?, ?> syncLine = findSyncLine(localId)
                .orElseThrow(() -> new IllegalArgumentException("No sync line found for " + localId));

        try {
            SyncId externalId = syncLine.push(localId);

            return SyncRecord.success(localId, externalId);
        } catch (Exception e) {
            return SyncRecord.failure(localId, null);
        }
    }


    private Optional<SyncLine<?, ?>> findSyncLine(SyncId syncId) {
        return syncSources.stream().filter(t -> t.matches(syncId)).findAny();
    }
}

