package com.klabis.sync.application;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.SyncAttempt;
import com.klabis.sync.domain.SyncOutcome;
import com.klabis.sync.domain.SyncTriggerKind;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetrySchedulerTest {

    private final SyncProperties properties = new SyncProperties();
    private final RetryScheduler scheduler = new RetryScheduler(properties);
    private static final SyncRecordId RECORD_ID = SyncRecordId.newId();

    private static SyncAttempt attempt(SyncOutcome outcome) {
        return SyncAttempt.record(RECORD_ID, SyncTriggerKind.SCHEDULED, null, outcome, null, null, null, null);
    }

    @Test
    void failedAttemptsSince_noHistory_zero() {
        assertThat(scheduler.failedAttemptsSince(List.of())).isZero();
    }

    @Test
    void failedAttemptsSince_stopsAtMostRecentSuccess() {
        List<SyncAttempt> history = List.of(
                attempt(SyncOutcome.FAILED),
                attempt(SyncOutcome.FAILED),
                attempt(SyncOutcome.SUCCESS),
                attempt(SyncOutcome.FAILED)
        );

        assertThat(scheduler.failedAttemptsSince(history)).isEqualTo(2);
    }

    @Test
    void failedAttemptsSince_stopsAtMostRecentReset() {
        List<SyncAttempt> history = List.of(
                attempt(SyncOutcome.FAILED),
                attempt(SyncOutcome.RESET),
                attempt(SyncOutcome.FAILED),
                attempt(SyncOutcome.FAILED)
        );

        assertThat(scheduler.failedAttemptsSince(history)).isEqualTo(1);
    }

    @Test
    void failedAttemptsSince_ignoresOutageRows() {
        List<SyncAttempt> history = List.of(
                attempt(SyncOutcome.FAILED),
                attempt(SyncOutcome.OUTAGE),
                attempt(SyncOutcome.FAILED),
                attempt(SyncOutcome.OUTAGE),
                attempt(SyncOutcome.SUCCESS)
        );

        assertThat(scheduler.failedAttemptsSince(history)).isEqualTo(2);
    }

    @Test
    void failedAttemptsSince_allFailuresNoSuccessOrReset_countsAll() {
        List<SyncAttempt> history = List.of(
                attempt(SyncOutcome.FAILED),
                attempt(SyncOutcome.FAILED),
                attempt(SyncOutcome.FAILED)
        );

        assertThat(scheduler.failedAttemptsSince(history)).isEqualTo(3);
    }

    @Test
    void nextAttemptDueAfter_firstFailure_initialDelay() {
        Instant now = Instant.now();

        Instant due = scheduler.nextAttemptDueAfter(1, now);

        assertThat(due).isEqualTo(now.plus(Duration.ofMinutes(15)));
    }

    @Test
    void nextAttemptDueAfter_secondFailure_delayDoubles() {
        Instant now = Instant.now();

        Instant due = scheduler.nextAttemptDueAfter(2, now);

        assertThat(due).isEqualTo(now.plus(Duration.ofMinutes(30)));
    }

    @Test
    void nextAttemptDueAfter_manyFailures_cappedAtMax() {
        Instant now = Instant.now();

        Instant due = scheduler.nextAttemptDueAfter(20, now);

        assertThat(due).isEqualTo(now.plus(Duration.ofHours(24)));
    }

    @Test
    void nextAttemptDueAfterOutage_alwaysInitialDelay() {
        Instant now = Instant.now();

        Instant due = scheduler.nextAttemptDueAfterOutage(now);

        assertThat(due).isEqualTo(now.plus(Duration.ofMinutes(15)));
    }

    @Test
    void hasReachedLimit_belowMaxAttempts_false() {
        assertThat(scheduler.hasReachedLimit(4)).isFalse();
    }

    @Test
    void hasReachedLimit_atMaxAttempts_true() {
        assertThat(scheduler.hasReachedLimit(5)).isTrue();
    }
}
