package com.klabis.sync.fixtures;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A {@link Clock} whose instant is fixed but can be advanced on demand, for tests
 * that need to simulate the passage of time without touching storage. Replaces the
 * old pattern of rewriting {@code started_at} / {@code next_attempt_due_at} columns
 * through {@code JdbcTemplate} to fake a record ageing past a threshold.
 */
public final class MutableClock extends Clock {

    private final ZoneId zone;
    private Instant instant;

    public MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    public MutableClock(Instant instant) {
        this(instant, ZoneId.systemDefault());
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    /** Moves the clock forward by {@code amount}. */
    public void advanceBy(Duration amount) {
        this.instant = this.instant.plus(amount);
    }

    /** Repoints the clock to {@code newInstant}. */
    public void setInstant(Instant newInstant) {
        this.instant = newInstant;
    }
}
