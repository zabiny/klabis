package com.klabis.sync.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Operational limits for the synchronisation engine, as configuration rather than
 * constants (design.md D19). Defaults match the D19 table.
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

    /**
     * Cron expression for the nightly full pass, re-comparing every active record.
     */
    private String scanCron = "0 0 2 * * *";

    /**
     * Cadence of the frequent due scan, picking up dirty or retry-due records.
     */
    private Duration dueScanInterval = Duration.ofMinutes(15);

    /**
     * How long a {@code sync_attempt} row is kept before the retention job deletes it.
     */
    private Duration historyRetention = Duration.ofDays(30);

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

    public String getScanCron() {
        return scanCron;
    }

    public void setScanCron(String scanCron) {
        this.scanCron = scanCron;
    }

    public Duration getDueScanInterval() {
        return dueScanInterval;
    }

    public void setDueScanInterval(Duration dueScanInterval) {
        this.dueScanInterval = dueScanInterval;
    }

    public Duration getHistoryRetention() {
        return historyRetention;
    }

    public void setHistoryRetention(Duration historyRetention) {
        this.historyRetention = historyRetention;
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
