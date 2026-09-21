package com.klabis.events.infrastructure.orissync;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Operational configuration for {@link DisciplineDiscoveryJob} (design.md D4). Kept
 * separate from {@code com.klabis.sync.application.SyncProperties} deliberately —
 * discipline discovery runs on its own schedule, independent of the sync engine's
 * {@code klabis.sync.scan-cron}/{@code due-scan-interval} — following the same
 * {@code @ConfigurationProperties}-with-coded-default convention {@code SyncProperties}
 * already uses.
 */
@ConfigurationProperties(prefix = "klabis.disciplines")
class DisciplinesProperties {

    /**
     * Cron expression for the nightly discovery pass, offset from the sync engine's
     * own {@code scan-cron} ({@code 0 0 2 * * *}) so the two jobs don't contend.
     */
    private String discoveryCron = "0 0 3 * * *";

    public String getDiscoveryCron() {
        return discoveryCron;
    }

    public void setDiscoveryCron(String discoveryCron) {
        this.discoveryCron = discoveryCron;
    }
}
