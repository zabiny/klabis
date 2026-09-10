package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.time.Instant;

/**
 * What a domain method's outcome should do to a record's {@link SyncSchedule}
 * (proposal.md "Domain methods stop mutating scheduling state and start returning
 * it"). {@code SyncRecord} no longer owns {@code dirtySince}/{@code nextAttemptDueAt}
 * directly; instead each method that used to mutate them returns the effect it wants,
 * and the application layer applies it through {@code SyncScheduleRepository} in the
 * same transaction as the record and attempt row (design.md D15).
 * <p>
 * {@code kind} names which of the five shapes this is; {@code value} carries the
 * instant for {@link Kind#DUE_AT} and {@link Kind#DIRTY_SINCE}, and is {@code null}
 * for the other three.
 */
@ValueObject
public record ScheduleEffect(Kind kind, Instant value) {

    public enum Kind {
        /** No scheduling change (proposal.md task 1.3: every path not listed there). */
        NO_CHANGE,
        /** Both {@code dirtySince} and {@code nextAttemptDueAt} are cleared. */
        CLEAR,
        /** Only {@code nextAttemptDueAt} is cleared; {@code dirtySince} is left standing. */
        CLEAR_DUE_AT,
        /** {@code nextAttemptDueAt} is set to {@link #value}. */
        DUE_AT,
        /** {@code dirtySince} is set to {@link #value}. */
        DIRTY_SINCE
    }

    public static ScheduleEffect noChange() {
        return new ScheduleEffect(Kind.NO_CHANGE, null);
    }

    public static ScheduleEffect clear() {
        return new ScheduleEffect(Kind.CLEAR, null);
    }

    public static ScheduleEffect clearDueAt() {
        return new ScheduleEffect(Kind.CLEAR_DUE_AT, null);
    }

    public static ScheduleEffect dueAt(Instant nextAttemptDueAt) {
        Assert.notNull(nextAttemptDueAt, "nextAttemptDueAt is required");
        return new ScheduleEffect(Kind.DUE_AT, nextAttemptDueAt);
    }

    public static ScheduleEffect dirtySince(Instant dirtySince) {
        Assert.notNull(dirtySince, "dirtySince is required");
        return new ScheduleEffect(Kind.DIRTY_SINCE, dirtySince);
    }

    /**
     * Applies this effect to a schedule, producing the next state. {@code sync}
     * package-private: only {@link SyncSchedule} and its repository need this — the
     * application layer treats a {@code ScheduleEffect} as an opaque instruction to
     * hand to {@code SyncScheduleRepository}, never applying it itself.
     * <p>
     * {@code DIRTY_SINCE} keeps {@code schedule.dirtySince()} when it is already set —
     * mirroring {@code SyncScheduleJdbcRepository.updateDirtySince}'s
     * {@code COALESCE(dirty_since, :dirtySince)}. Without this, this in-memory mirror
     * would report a different {@code dirtySince} than the row a concurrent
     * {@code DIRTY_SINCE} effect just persisted, even though both only ever get read as
     * null/non-null (never for its actual age).
     */
    SyncSchedule applyTo(SyncSchedule schedule) {
        return switch (kind) {
            case NO_CHANGE -> schedule;
            case CLEAR -> new SyncSchedule(null, null);
            case CLEAR_DUE_AT -> new SyncSchedule(schedule.dirtySince(), null);
            case DUE_AT -> new SyncSchedule(schedule.dirtySince(), value);
            case DIRTY_SINCE -> new SyncSchedule(
                    schedule.dirtySince() != null ? schedule.dirtySince() : value,
                    schedule.nextAttemptDueAt());
        };
    }
}
