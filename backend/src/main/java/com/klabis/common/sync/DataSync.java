package com.klabis.common.sync;

/**
 * Interface for data synchronization. Allows to trigger synchronization in either direction using just SyncId ID.
 */
public interface DataSync {

    enum SyncResult {
        SYNCED, ERROR
    }

    enum Direction { PUSH, PULL }

    SyncRecord sync(SyncItemId syncItemId, Direction direction);

    default SyncRecord sync(SyncItemId syncItemId) {
        return sync(syncItemId, syncItemId.isLocalId() ? Direction.PUSH : Direction.PULL);
    }
}
