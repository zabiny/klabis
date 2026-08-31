package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * The outcome of comparing a record's current local/external snapshots against its
 * baseline (design.md D4's decision table). {@code kind} names the row; {@code direction}
 * is only meaningful for {@link Kind#WRITE}.
 * <p>
 * Slice 1 produced {@link Kind#NOTHING_TO_DO} and inward {@link Kind#WRITE} (via
 * {@link Kind#ADOPT_EXTERNAL} on first enrolment or {@link Kind#WRITE} thereafter).
 * Slice 2 adds outward {@link Kind#WRITE} and {@link Kind#CONVERGED}. Conflicts are
 * added by Slice 3 without changing this shape.
 */
@ValueObject
public record SyncDecision(Kind kind, SyncDirection direction) {

    public enum Kind {
        /** Neither side changed since the baseline. */
        NOTHING_TO_DO,
        /** No baseline exists yet — first pass adopts the external side (design.md D5). */
        ADOPT_EXTERNAL,
        /** One side changed and the write in that direction is available. */
        WRITE,
        /**
         * Both sides changed since the baseline but now hold the same value — rebase
         * both baselines onto the shared state, write nothing (design.md D4).
         */
        CONVERGED
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

    public static SyncDecision converged() {
        return new SyncDecision(Kind.CONVERGED, null);
    }
}
