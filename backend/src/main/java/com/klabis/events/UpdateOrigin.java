package com.klabis.events;

/**
 * Identifies which code path performed an {@code Event} update.
 *
 * <p>Must always be set to reflect the actual code path that produced the write —
 * never inferred from data. A misclassified {@code MANUAL} update as
 * {@code SYNCHRONISATION} would let a genuine local change go unsynchronised.
 */
public enum UpdateOrigin {
    MANUAL,
    SYNCHRONISATION
}
