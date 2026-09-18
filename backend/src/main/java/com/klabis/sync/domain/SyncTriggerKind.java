package com.klabis.sync.domain;

/**
 * What started a synchronisation pass.
 */
public enum SyncTriggerKind {
    SCHEDULED,
    LOCAL_CHANGE,
    MANUAL
}
