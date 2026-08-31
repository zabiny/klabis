package com.klabis.sync.domain;

/**
 * The external system a synchronisation record is paired against.
 * <p>
 * The engine depends on no other module's identifier type; this enum is the whole
 * of what it knows about an external system (design.md D1).
 */
public enum ExternalSystem {
    ORIS
}
