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
    public SyncRecord sync(SyncId syncId, Direction direction) {
        SyncLine<?, ?> syncLine = findSyncLine(syncId)
                .orElseThrow(() -> new IllegalArgumentException("No sync line found for " + syncId));
        return direction == Direction.PUSH
                ? syncPush(syncId, syncLine)
                : syncPull(syncId, syncLine);
    }

    /**
     * To push, the local ID is required. When the caller holds an external ID, it is translated to
     * the local ID via the existing {@link SyncRecord} for the pair.
     */
    private SyncRecord syncPush(SyncId syncId, SyncLine<?, ?> syncLine) {
        SyncId localId = null;
        try {
            localId = resolveLocalId(syncId);
            SyncId externalId = syncLine.push(localId);
            return persist(SyncRecord.success(localId, externalId));
        } catch (Exception e) {
            return persistFailure(syncId, localId, null, e);
        }
    }

    /**
     * To pull, the external ID is required. When the caller holds a local ID, it is translated to
     * the external ID via the existing {@link SyncRecord} for the pair.
     */
    private SyncRecord syncPull(SyncId syncId, SyncLine<?, ?> syncLine) {
        SyncId externalId = null;
        try {
            externalId = resolveExternalId(syncId);
            SyncId localId = syncLine.pull(externalId);
            return persist(SyncRecord.success(localId, externalId));
        } catch (Exception e) {
            return persistFailure(syncId, null, externalId, e);
        }
    }

    private SyncId resolveLocalId(SyncId syncId) {
        if (syncId.isLocalId()) {
            return syncId;
        }
        Optional<SyncRecord> record = syncRecords.findById(syncId);
        if (record.isEmpty()) {
            throw new IllegalStateException("No sync record found for " + syncId);
        }
        SyncId localId = record.get().localId();
        if (localId == null) {
            throw new IllegalStateException("Sync record for " + syncId + " has no local ID");
        }
        return localId;
    }

    private SyncId resolveExternalId(SyncId syncId) {
        if (syncId.isExternalId()) {
            return syncId;
        }
        Optional<SyncRecord> record = syncRecords.findById(syncId);
        if (record.isEmpty()) {
            throw new IllegalStateException("No sync record found for " + syncId);
        }
        SyncId externalId = record.get().externalId();
        if (externalId == null) {
            throw new IllegalStateException("Sync record for " + syncId + " has no external ID");
        }
        return externalId;
    }

    private SyncRecord persistFailure(SyncId syncId, SyncId resolvedLocalId, SyncId resolvedExternalId, Exception cause) {
        log.warn("Sync failed for {}", syncId, cause);
        SyncId localId = resolvedLocalId != null ? resolvedLocalId : (syncId.isLocalId() ? syncId : null);
        SyncId externalId = resolvedExternalId != null ? resolvedExternalId : (syncId.isExternalId() ? syncId : null);
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
        SyncId lookup = record.localId() != null ? record.localId() : record.externalId();
        return syncRecords.findById(lookup)
                .filter(existing -> Objects.equals(existing.localId(), record.localId())
                        && Objects.equals(existing.externalId(), record.externalId()))
                .map(SyncRecord::id);
    }

    private Optional<SyncLine<?, ?>> findSyncLine(SyncId syncId) {
        return syncLines.stream().filter(t -> t.matches(syncId)).findAny();
    }
}
