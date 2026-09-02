package com.klabis.sync.domain;

/**
 * A manager's resolution of a standing conflict (design.md D6): force a direction, or
 * accept that the two sides deliberately differ.
 */
public enum SyncResolution {
    INWARD,
    OUTWARD,
    ACCEPT_DIVERGENCE
}
