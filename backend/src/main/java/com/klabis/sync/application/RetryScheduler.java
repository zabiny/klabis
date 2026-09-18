package com.klabis.sync.application;

import com.klabis.sync.domain.SyncAttempt;
import com.klabis.sync.domain.SyncOutcome;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Derives the retry state from a record's attempt history (design.md D10): the
 * failure count since the most recent success or reset, and the next-attempt-due
 * delay grown from it.
 * <p>
 * The count is derived rather than denormalised onto the record — one source of
 * truth, no drift (design.md D10) — at the cost of walking the history newest-first
 * until a {@code SUCCESS} or {@code RESET} row, or the list ends. {@code OUTAGE} rows
 * are skipped entirely: an outage counts toward neither the failure count nor the
 * backoff delay (design.md D11).
 */
class RetryScheduler {

    private final SyncProperties properties;

    RetryScheduler(SyncProperties properties) {
        this.properties = properties;
    }

    /**
     * Attempts since the most recent {@code SUCCESS} or {@code RESET}, ignoring
     * {@code OUTAGE} rows (design.md D10, D11). History is expected newest-first, as
     * {@link com.klabis.sync.domain.SyncAttemptRepository#findByRecordIdOrderByStartedAtDesc}
     * returns it.
     */
    int failedAttemptsSince(List<SyncAttempt> historyNewestFirst) {
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

    /**
     * The next-attempt-due instant for a record that has just failed its
     * {@code failedAttemptsSoFar}-th retryable attempt in a row (design.md D19):
     * {@code initial * multiplier^(failedAttemptsSoFar - 1)}, capped at {@code max}.
     * The first failure (count 1) waits the initial delay; each further failure grows
     * it by the multiplier.
     */
    Instant nextAttemptDueAfter(int failedAttemptsSoFar, Instant now) {
        Duration initial = properties.getRetryDelay().getInitial();
        Duration max = properties.getRetryDelay().getMax();
        double multiplier = properties.getRetryDelay().getMultiplier();

        double growth = Math.pow(multiplier, Math.max(0, failedAttemptsSoFar - 1));
        Duration delay = Duration.ofMillis(Math.round(initial.toMillis() * growth));
        if (delay.compareTo(max) > 0) {
            delay = max;
        }
        return now.plus(delay);
    }

    /**
     * The next-attempt-due instant after an outage (design.md D11): always the
     * initial delay, never the grown one — an outage failure does not count toward
     * the backoff, so which records were attempted during it stops mattering.
     */
    Instant nextAttemptDueAfterOutage(Instant now) {
        return now.plus(properties.getRetryDelay().getInitial());
    }

    boolean hasReachedLimit(int failedAttempts) {
        return failedAttempts >= properties.getMaxAttempts();
    }
}
