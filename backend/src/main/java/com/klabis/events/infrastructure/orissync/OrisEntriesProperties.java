package com.klabis.events.infrastructure.orissync;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Operational configuration of the ORIS registration synchronisation: the discovery schedule of
 * {@link OrisEventRegistrationDiscoveryJob} and the read cache of {@link OrisEventEntries}.
 */
@ConfigurationProperties(prefix = "klabis.events")
class OrisEntriesProperties {

    private String orisEntriesDiscoveryCron = "0 30 * * * *";

    private Duration orisEntriesCacheTtl = Duration.ofMinutes(5);

    public String getOrisEntriesDiscoveryCron() {
        return orisEntriesDiscoveryCron;
    }

    public void setOrisEntriesDiscoveryCron(String orisEntriesDiscoveryCron) {
        this.orisEntriesDiscoveryCron = orisEntriesDiscoveryCron;
    }

    public Duration getOrisEntriesCacheTtl() {
        return orisEntriesCacheTtl;
    }

    public void setOrisEntriesCacheTtl(Duration orisEntriesCacheTtl) {
        this.orisEntriesCacheTtl = orisEntriesCacheTtl;
    }
}
