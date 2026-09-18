package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Instant;

/**
 * When a record was last observed to change ({@code dirtySince}) and when its next
 * retry is due ({@code nextAttemptDueAt}) — design.md D9, D10. Purely a scheduling
 * hint, never consulted for correctness: it marks a record due for the due scan and
 * collapses bursts of edits, and correctness instead comes from re-reading the local
 * projection at the decision points ({@code SynchronizationService}).
 * <p>
 * Deliberately not versioned (proposal.md task 2.3): a scheduling write must never
 * contend for the same optimistic lock as the record's own domain state.
 */
@ValueObject
public record SyncSchedule(Instant dirtySince, Instant nextAttemptDueAt) {

    /**
     * The coded default for a record with no schedule row yet — every record gets one
     * at enrolment (proposal.md task 2.6), so this exists only as a defensive fallback,
     * never as the steady-state result of a normal read.
     */
    public static SyncSchedule empty() {
        return new SyncSchedule(null, null);
    }

    /**
     * Produces the next schedule state after applying {@code effect}. Public: the
     * JDBC adapter (a different package) is the one that reads the current schedule,
     * applies the effect, and persists the result.
     */
    public SyncSchedule apply(ScheduleEffect effect) {
        return effect.applyTo(this);
    }
}
