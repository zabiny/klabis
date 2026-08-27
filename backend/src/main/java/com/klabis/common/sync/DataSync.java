package com.klabis.common.sync;

public interface DataSync {

    enum SyncResult {
        SYNCED, ERROR
    }

    enum Direction { PUSH, PULL }

    SyncRecord sync(SyncId syncId, Direction direction);

    default SyncRecord sync(SyncId syncId) {
        return sync(syncId, syncId.isLocalId() ? Direction.PUSH : Direction.PULL);
    }
}
