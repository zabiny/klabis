package com.klabis.sync.domain;

/**
 * The lifecycle state of one {@link SyncRecord}.
 */
public enum SyncStatus {
    NEW,
    IN_SYNC,
    RETRYING,
    CONFLICT,
    FAILED,
    RETIRED
}
