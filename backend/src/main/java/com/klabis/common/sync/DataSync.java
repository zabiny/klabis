package com.klabis.common.sync;

/**
 * Interface for data synchronization. Allows to trigger synchronization in either direction using just SyncId ID.
 */
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
