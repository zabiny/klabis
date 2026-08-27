package com.klabis.common.sync;

import org.springframework.data.annotation.Id;

public record SyncRecord(@Id java.util.UUID id, SyncId localId, SyncId externalId,
                         DataSync.SyncResult result, String failureCause) {

    public SyncRecord {
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
            id = java.util.UUID.randomUUID();
        }
    }

    public static SyncRecord success(SyncId localId, SyncId externalId) {
        return new SyncRecord(null, localId, externalId, DataSync.SyncResult.SYNCED, null);
    }

    public static SyncRecord failure(SyncId localId, SyncId externalId, String failureCause) {
        return new SyncRecord(null, localId, externalId, DataSync.SyncResult.ERROR, failureCause);
    }
}
