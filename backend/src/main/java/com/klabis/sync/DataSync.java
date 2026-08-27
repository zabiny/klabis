package com.klabis.sync;

public interface DataSync {

    enum SyncResult {
        SYNCED, NOT_CHANGED, ERROR
    }

    SyncRecord sync(SyncId syncId) throws DataSyncException;

    class DataSyncException extends RuntimeException {
        public DataSyncException(String message) {
            super(message);
        }
    }
}
