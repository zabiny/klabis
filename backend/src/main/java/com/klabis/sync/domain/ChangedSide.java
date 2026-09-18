package com.klabis.sync.domain;

/**
 * Per-field conflict attribution: which side moved away from the baseline
 * (design.md D4 response shape, D14).
 */
public enum ChangedSide {
    LOCAL,
    EXTERNAL,
    BOTH
}
