package com.klabis.sync.application;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RetrySchedulerTest {

    private final SyncProperties properties = new SyncProperties();
    private final RetryScheduler scheduler = new RetryScheduler(properties);

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
