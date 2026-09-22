package com.klabis.members.infrastructure.orissync;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Operational configuration for {@link MemberDiscoveryJob} (design.md D7). Kept
 * separate from {@code com.klabis.sync.application.SyncProperties} and from
 * {@code com.klabis.events.infrastructure.orissync.DisciplinesProperties} deliberately
 * — member discovery runs on its own schedule, independent of both, following the same
 * {@code @ConfigurationProperties}-with-coded-default convention they already use.
 */
@ConfigurationProperties(prefix = "klabis.members")
class MembersOrisDiscoveryProperties {

    /**
     * Cron expression for the member discovery pass.
     */
    private String orisDiscoveryCron = "0 30 3 * * *";

    public String getOrisDiscoveryCron() {
        return orisDiscoveryCron;
    }

    public void setOrisDiscoveryCron(String orisDiscoveryCron) {
        this.orisDiscoveryCron = orisDiscoveryCron;
    }
}
