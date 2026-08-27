package com.klabis.common.sync;

import org.springframework.data.annotation.Id;

public record SyncRecord(@Id java.util.UUID id, SyncId localId, SyncId externalId,
                         DataSync.SyncResult result, Throwable failureException) {

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
        if (result == DataSync.SyncResult.ERROR && failureException == null) {
            throw new IllegalArgumentException("ERROR result requires a failureException");
        }
        if (result == DataSync.SyncResult.SYNCED && failureException != null) {
            throw new IllegalArgumentException("SYNCED result must not carry a failureException");
        }
        if (id == null) {
            id = java.util.UUID.randomUUID();
        }
    }

    public static SyncRecord success(SyncId localId, SyncId externalId) {
        return new SyncRecord(null, localId, externalId, DataSync.SyncResult.SYNCED, null);
    }

    public static SyncRecord failure(SyncId localId, SyncId externalId, Throwable failureException) {
        return new SyncRecord(null, localId, externalId, DataSync.SyncResult.ERROR, failureException);
    }
}
