package com.klabis.sync.application;

/**
 * Narrows {@link SyncHistoryQuery#listAttempts}. An attempt "changed nothing" when it is an
 * automatic pass that succeeded without writing to either side; such attempts dominate the
 * history, so they are hidden unless asked for.
 */
public record AttemptFilter(boolean includeUnchanged) {

    public static AttemptFilter changedOnly() {
        return new AttemptFilter(false);
    }

    public static AttemptFilter all() {
        return new AttemptFilter(true);
    }
}
