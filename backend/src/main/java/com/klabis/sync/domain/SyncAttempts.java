package com.klabis.sync.domain;

import java.util.List;

public final class SyncAttempts {

    private SyncAttempts() {
    }

    /**
     * Attempts since the most recent {@code SUCCESS} or {@code RESET}, ignoring {@code OUTAGE}
     * rows (design.md D10, D11).
     *
     * @param historyNewestFirst as {@link SyncAttemptRepository#findByRecordIdOrderByStartedAtDesc} returns it
     */
    public static int failedSinceLastSuccess(List<SyncAttempt> historyNewestFirst) {
        int count = 0;
        for (SyncAttempt attempt : historyNewestFirst) {
            if (attempt.getOutcome() == SyncOutcome.SUCCESS || attempt.getOutcome() == SyncOutcome.RESET) {
                break;
            }
            if (attempt.getOutcome() == SyncOutcome.OUTAGE) {
                continue;
            }
            count++;
        }
        return count;
    }
}
