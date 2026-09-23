package com.klabis.common.settings;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * The cluster-wide secret setting does not survive a restart (design.md D9, Risks): held
 * only in a field of a running instance. A persistent encrypted store is the expected
 * follow-up, swapped in behind {@link OrisClubKeyPort} without touching callers.
 * <p>
 * A value supplied out of band (e.g. an environment variable surfaced as
 * {@code klabis.oris.club.key}) seeds the setting at startup; without one no key is held,
 * exactly as if it had never been supplied. The default is deliberately empty rather than
 * required, so the application starts normally with no key and {@link #isSet()} reports
 * the absence plainly.
 */
@SecondaryAdapter
@Component
class InMemoryOrisClubKeyAdapter implements OrisClubKeyPort, OrisClubKeyAccessor {

    @Value("${klabis.oris.club.key:}")
    private volatile String clubKey;

    @Override
    public void store(String clubKey) {
        Assert.hasText(clubKey, "Club key must not be blank");
        this.clubKey = clubKey;
    }

    @Override
    public boolean isSet() {
        return clubKey != null && !clubKey.isBlank();
    }

    @Override
    public void clear() {
        this.clubKey = null;
    }

    /**
     * Read side of the key, declared on {@link OrisClubKeyAccessor}. Only a component that
     * attaches the key to an outgoing request — {@code DefaultOrisClubMembers} — is meant
     * to call it; {@link OrisClubKeyPort} itself carries no getter.
     */
    @Override
    public String currentKey() {
        return clubKey;
    }
}
