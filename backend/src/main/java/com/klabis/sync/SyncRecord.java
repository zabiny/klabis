package com.klabis.sync;

import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.data.annotation.Id;

import java.util.Objects;
import java.util.UUID;

@RecordBuilder
public final class SyncRecord {
    @Id
    private final UUID id;
    private final SyncId localId;
    private final SyncId externalId;
    private DataSync.SyncResult result;


    public SyncRecord(UUID id, SyncId localId, SyncId externalId, DataSync.SyncResult result) {
        if (localId == null && externalId == null) {
            throw new IllegalArgumentException("Either localId or externalId must be set");
        }
        if (localId != null && !localId.isLocalId()) {
            throw new IllegalArgumentException("localId must be a local ID");
        }
        if (externalId != null && !externalId.isExternalId()) {
            throw new IllegalArgumentException("externalId must be an external ID");
        }
        if (id == null) {
            id = UUID.randomUUID();
        }
        this.id = id;
        this.localId = localId;
        this.externalId = externalId;
        this.result = result;
    }

    public static SyncRecord failure(SyncId localId, SyncId externalId) {
        return new SyncRecord(UUID.randomUUID(), localId, externalId, DataSync.SyncResult.ERROR);
    }

    public static SyncRecord success(SyncId localId, SyncId externalId) {
        return new SyncRecord(UUID.randomUUID(), localId, externalId, DataSync.SyncResult.SYNCED);
    }

    @Id
    public UUID id() {
        return id;
    }

    public SyncId localId() {
        return localId;
    }

    public SyncId externalId() {
        return externalId;
    }

    public DataSync.SyncResult result() {
        return result;
    }

    public void markAsSynced() {
        this.result = DataSync.SyncResult.SYNCED;
    }

    public void markAsError() {
        this.result = DataSync.SyncResult.ERROR;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SyncRecord) obj;
        return Objects.equals(this.id, that.id) &&
               Objects.equals(this.localId, that.localId) &&
               Objects.equals(this.externalId, that.externalId) &&
               Objects.equals(this.result, that.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SyncRecord[" +
               "id=" + id + ", " +
               "localId=" + localId + ", " +
               "externalId=" + externalId + ", " +
               "result=" + result + ']';
    }


}
