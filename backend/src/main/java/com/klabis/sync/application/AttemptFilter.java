package com.klabis.sync.application;

import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncOutcome;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Narrows {@link SyncHistoryQuery#listAttempts}. An attempt "changed nothing" when it is an
 * automatic pass that succeeded without writing to either side; such attempts dominate the
 * history, so they are hidden unless asked for. {@code from} is inclusive, {@code to} exclusive.
 */
public record AttemptFilter(
        boolean includeUnchanged,
        @Nullable SyncEntityType entityType,
        @Nullable SyncOutcome outcome,
        @Nullable Instant from,
        @Nullable Instant to) {

    public static AttemptFilter changedOnly() {
        return new AttemptFilter(false, null, null, null, null);
    }

    public static AttemptFilter all() {
        return new AttemptFilter(true, null, null, null, null);
    }

    public AttemptFilter withIncludeUnchanged(boolean includeUnchanged) {
        return new AttemptFilter(includeUnchanged, entityType, outcome, from, to);
    }

    public AttemptFilter withEntityType(@Nullable SyncEntityType entityType) {
        return new AttemptFilter(includeUnchanged, entityType, outcome, from, to);
    }

    public AttemptFilter withOutcome(@Nullable SyncOutcome outcome) {
        return new AttemptFilter(includeUnchanged, entityType, outcome, from, to);
    }

    public AttemptFilter withFrom(@Nullable Instant from) {
        return new AttemptFilter(includeUnchanged, entityType, outcome, from, to);
    }

    public AttemptFilter withTo(@Nullable Instant to) {
        return new AttemptFilter(includeUnchanged, entityType, outcome, from, to);
    }
}
