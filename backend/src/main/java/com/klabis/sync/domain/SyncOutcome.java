package com.klabis.sync.domain;

/**
 * How one recorded {@link SyncAttempt} ended.
 */
public enum SyncOutcome {
    SUCCESS,
    CONFLICT,
    FAILED,
    OUTAGE,
    RESET,
    SKIPPED
}
