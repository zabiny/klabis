package com.klabis.sync.application;

/**
 * How an entity is paired with an external system, across all systems: {@code ACTIVE} if any record is
 * not retired, otherwise {@code RETIRED} if any record exists, otherwise {@code NONE}.
 */
public enum SyncPairing {
    NONE,
    ACTIVE,
    RETIRED
}
