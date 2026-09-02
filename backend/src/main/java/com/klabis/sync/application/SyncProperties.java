package com.klabis.sync.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Operational limits for the synchronisation engine, as configuration rather than
 * constants (design.md D19). Defaults match the D19 table.
 * <p>
 * Slice 4 introduces {@code max-attempts}, {@code claim-lease} and {@code retry-delay};
 * {@code scan-cron}, {@code due-scan-interval} and {@code history-retention} are added
 * by Slice 5 alongside the scheduler that consumes them.
 */
@ConfigurationProperties(prefix = "klabis.sync")
class SyncProperties {

    /**
     * Retryable attempts since the last success or reset before the record becomes
     * terminally failed.
     */
    private int maxAttempts = 5;

    /**
     * How long a claim holds a record before another pass may take it.
     */
    private Duration claimLease = Duration.ofMinutes(5);

    private final RetryDelay retryDelay = new RetryDelay();

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getClaimLease() {
        return claimLease;
    }

    public void setClaimLease(Duration claimLease) {
        this.claimLease = claimLease;
    }

    public RetryDelay getRetryDelay() {
        return retryDelay;
    }

    static class RetryDelay {

        /**
         * Delay before the first retry of a failed record.
         */
        private Duration initial = Duration.ofMinutes(15);

        /**
         * Growth factor per consecutive failure.
         */
        private double multiplier = 2;

        /**
         * Ceiling for the delay.
         */
        private Duration max = Duration.ofHours(24);

        public Duration getInitial() {
            return initial;
        }

        public void setInitial(Duration initial) {
            this.initial = initial;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public Duration getMax() {
            return max;
        }

        public void setMax(Duration max) {
            this.max = max;
        }
    }
}
