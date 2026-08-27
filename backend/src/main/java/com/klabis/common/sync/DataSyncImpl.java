package com.klabis.common.sync;

import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class DataSyncImpl implements DataSync {

    private static final Logger log = LoggerFactory.getLogger(DataSyncImpl.class);

    private final Collection<SyncLine<?, ?>> syncLines;
    private final SyncRecordRepository syncRecords;

    public DataSyncImpl(Collection<SyncLine<?, ?>> syncLines, SyncRecordRepository syncRecords) {
        this.syncLines = syncLines;
        this.syncRecords = syncRecords;
    }

    @Override
    public SyncRecord sync(SyncItemId syncItemId, Direction direction) {
        SyncLine<?, ?> syncLine = findSyncLine(syncItemId)
                .orElseThrow(() -> new IllegalArgumentException("No sync line found for " + syncItemId));

        return direction == Direction.PUSH
                ? syncPush(syncItemId, syncLine)
                : syncPull(syncItemId, syncLine);
    }

    /**
     * To push, the local ID is required. When the caller holds an external ID, it is translated to
     * the local ID via the existing {@link SyncRecord} for the pair.
     */
    private SyncRecord syncPush(SyncItemId syncItemId, SyncLine<?, ?> syncLine) {
        SyncItemId localId = null;
        try {
            localId = resolveLocalId(syncItemId);
            SyncItemId externalId = syncLine.push(localId);
            return persist(SyncRecord.success(localId, externalId));
        } catch (Exception e) {
            return persistFailure(syncItemId, localId, null, e);
        }
    }

    /**
     * To pull, the external ID is required. When the caller holds a local ID, it is translated to
     * the external ID via the existing {@link SyncRecord} for the pair.
     */
    private SyncRecord syncPull(SyncItemId syncItemId, SyncLine<?, ?> syncLine) {
        SyncItemId externalId = null;
        try {
            externalId = resolveExternalId(syncItemId);
            SyncItemId localId = syncLine.pull(externalId);
            return persist(SyncRecord.success(localId, externalId));
        } catch (Exception e) {
            return persistFailure(syncItemId, null, externalId, e);
        }
    }

    private SyncItemId resolveLocalId(SyncItemId syncItemId) {
        if (syncItemId.isLocalId()) {
            return syncItemId;
        }
        Optional<SyncRecord> record = syncRecords.findById(syncItemId);
        if (record.isEmpty()) {
            throw new IllegalStateException("No sync record found for " + syncItemId);
        }
        SyncItemId localId = record.get().localId();
        if (localId == null) {
            throw new IllegalStateException("Sync record for " + syncItemId + " has no local ID");
        }
        return localId;
    }

    private SyncItemId resolveExternalId(SyncItemId syncItemId) {
        if (syncItemId.isExternalId()) {
            return syncItemId;
        }
        Optional<SyncRecord> record = syncRecords.findById(syncItemId);
        if (record.isEmpty()) {
            throw new IllegalStateException("No sync record found for " + syncItemId);
        }
        SyncItemId externalId = record.get().externalId();
        if (externalId == null) {
            throw new IllegalStateException("Sync record for " + syncItemId + " has no external ID");
        }
        return externalId;
    }

    private SyncRecord persistFailure(SyncItemId syncItemId, SyncItemId resolvedLocalId, SyncItemId resolvedExternalId, Exception cause) {
        log.warn("Sync failed for {}", syncItemId, cause);
        SyncItemId localId = resolvedLocalId != null ? resolvedLocalId : (syncItemId.isLocalId() ? syncItemId : null);
        SyncItemId externalId = resolvedExternalId != null ? resolvedExternalId : (syncItemId.isExternalId() ? syncItemId : null);
        return persist(SyncRecord.failure(localId, externalId, cause));
    }

    /**
     * Upserts by the natural key {@code (localId, externalId)}: reuses the id of an existing record
     * for the same pair so a resync updates it in place instead of adding a duplicate row.
     */
    private SyncRecord persist(SyncRecord record) {
        SyncRecord toSave = existingIdFor(record)
                .map(id -> new SyncRecord(id, record.localId(), record.externalId(),
                        record.result(), record.failureException()))
                .orElse(record);
        return syncRecords.save(toSave);
    }

    private Optional<UUID> existingIdFor(SyncRecord record) {
        SyncItemId lookup = record.localId() != null ? record.localId() : record.externalId();
        return syncRecords.findById(lookup)
                .filter(existing -> Objects.equals(existing.localId(), record.localId())
                        && Objects.equals(existing.externalId(), record.externalId()))
                .map(SyncRecord::id);
    }

    private Optional<SyncLine<?, ?>> findSyncLine(SyncItemId syncItemId) {
        return syncLines.stream().filter(t -> t.canProcess(syncItemId)).findAny();
    }
}
