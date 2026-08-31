package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * The outcome of comparing a record's current local/external snapshots against its
 * baseline (design.md D4's decision table). {@code kind} names the row; {@code direction}
 * is only meaningful for {@link Kind#WRITE}.
 * <p>
 * Slice 1 produces {@link Kind#NOTHING_TO_DO} and {@link Kind#WRITE} (inward only, via
 * {@link Kind#ADOPT_EXTERNAL} on first enrolment or {@link Kind#WRITE} thereafter).
 * Outward writes, convergence and conflicts are added by later slices without changing
 * this shape.
 */
@ValueObject
public record SyncDecision(Kind kind, SyncDirection direction) {

    public enum Kind {
        /** Neither side changed since the baseline. */
        NOTHING_TO_DO,
        /** No baseline exists yet — first pass adopts the external side (design.md D5). */
        ADOPT_EXTERNAL,
        /** One side changed and the write in that direction is available. */
        WRITE
    }

    public static SyncDecision nothingToDo() {
        return new SyncDecision(Kind.NOTHING_TO_DO, null);
    }

    public static SyncDecision adoptExternal() {
        return new SyncDecision(Kind.ADOPT_EXTERNAL, SyncDirection.INWARD);
    }

    public static SyncDecision write(SyncDirection direction) {
        return new SyncDecision(Kind.WRITE, direction);
    }
}
