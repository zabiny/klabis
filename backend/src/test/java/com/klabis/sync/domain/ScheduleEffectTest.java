package com.klabis.sync.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The five {@link ScheduleEffect} shapes and how each applies to a {@link
 * SyncSchedule} (proposal.md task 2.1). This is the pure logic that will replace the
 * direct field mutation in {@code SyncRecord} once section 3 wires it in.
 */
@DisplayName("ScheduleEffect")
class ScheduleEffectTest {

    private static final Instant DIRTY_SINCE = Instant.parse("2026-09-01T10:00:00Z");
    private static final Instant NEXT_ATTEMPT_DUE_AT = Instant.parse("2026-09-01T11:00:00Z");
    private static final SyncSchedule BOTH_SET = new SyncSchedule(DIRTY_SINCE, NEXT_ATTEMPT_DUE_AT);

    @Test
    @DisplayName("noChange() leaves the schedule untouched")
    void noChangeLeavesScheduleUntouched() {
        SyncSchedule result = BOTH_SET.apply(ScheduleEffect.noChange());

        assertThat(result).isEqualTo(BOTH_SET);
    }

    @Test
    @DisplayName("clear() clears both dirtySince and nextAttemptDueAt")
    void clearClearsBoth() {
        SyncSchedule result = BOTH_SET.apply(ScheduleEffect.clear());

        assertThat(result.dirtySince()).isNull();
        assertThat(result.nextAttemptDueAt()).isNull();
    }

    @Test
    @DisplayName("clearDueAt() clears only nextAttemptDueAt, leaving dirtySince standing")
    void clearDueAtClearsOnlyDueAt() {
        SyncSchedule result = BOTH_SET.apply(ScheduleEffect.clearDueAt());

        assertThat(result.dirtySince()).isEqualTo(DIRTY_SINCE);
        assertThat(result.nextAttemptDueAt()).isNull();
    }

    @Test
    @DisplayName("dueAt(instant) sets nextAttemptDueAt, leaving dirtySince untouched")
    void dueAtSetsNextAttemptDueAt() {
        SyncSchedule startingPoint = new SyncSchedule(DIRTY_SINCE, null);
        Instant newDueAt = Instant.parse("2026-09-02T00:00:00Z");

        SyncSchedule result = startingPoint.apply(ScheduleEffect.dueAt(newDueAt));

        assertThat(result.dirtySince()).isEqualTo(DIRTY_SINCE);
        assertThat(result.nextAttemptDueAt()).isEqualTo(newDueAt);
    }

    @Test
    @DisplayName("dirtySince(instant) sets dirtySince, leaving nextAttemptDueAt untouched")
    void dirtySinceSetsDirtySince() {
        SyncSchedule startingPoint = new SyncSchedule(null, NEXT_ATTEMPT_DUE_AT);
        Instant newDirtySince = Instant.parse("2026-09-02T00:00:00Z");

        SyncSchedule result = startingPoint.apply(ScheduleEffect.dirtySince(newDirtySince));

        assertThat(result.dirtySince()).isEqualTo(newDirtySince);
        assertThat(result.nextAttemptDueAt()).isEqualTo(NEXT_ATTEMPT_DUE_AT);
    }

    @Test
    @DisplayName("dueAt(null) is rejected — a due date is always a concrete instant")
    void dueAtRejectsNull() {
        assertThatThrownBy(() -> ScheduleEffect.dueAt(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("dirtySince(null) is rejected — use clear()/clearDueAt() to remove a value")
    void dirtySinceRejectsNull() {
        assertThatThrownBy(() -> ScheduleEffect.dirtySince(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SyncSchedule.empty() starts with both fields null")
    void emptyScheduleStartsWithBothNull() {
        SyncSchedule empty = SyncSchedule.empty();

        assertThat(empty.dirtySince()).isNull();
        assertThat(empty.nextAttemptDueAt()).isNull();
    }
}
