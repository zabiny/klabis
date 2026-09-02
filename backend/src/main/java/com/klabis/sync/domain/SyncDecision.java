package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * The outcome of comparing a record's current local/external snapshots against its
 * baseline (design.md D4's decision table). {@code kind} names the row. For
 * {@link Kind#WRITE} and {@link Kind#ADOPT_EXTERNAL}, {@code direction} is the write
 * direction. For {@link Kind#CONFLICT}, it names the direction that was attempted and
 * blocked — {@code OUTWARD} when a local change had no outward write capability,
 * {@code null} for a two-sided conflict or the accepted-divergence guard, where no
 * single direction was attempted.
 * <p>
 * Slice 1 produced {@link Kind#NOTHING_TO_DO} and inward {@link Kind#WRITE} (via
 * {@link Kind#ADOPT_EXTERNAL} on first enrolment or {@link Kind#WRITE} thereafter).
 * Slice 2 added outward {@link Kind#WRITE} and {@link Kind#CONVERGED}. Slice 3 adds
 * {@link Kind#CONFLICT} — the last two rows of D4's table, plus the standing accepted
 * divergence guard (design.md D6).
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
        CONVERGED,
        /**
         * Both sides changed to different values, a local change cannot be written
         * outward, or the external side moved while a standing accepted divergence
         * protects the local value (design.md D4, D6). Never resolved by the system.
         */
        CONFLICT
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

    public static SyncDecision conflict() {
        return new SyncDecision(Kind.CONFLICT, null);
    }

    public static SyncDecision conflict(SyncDirection attemptedDirection) {
        return new SyncDecision(Kind.CONFLICT, attemptedDirection);
    }
}
