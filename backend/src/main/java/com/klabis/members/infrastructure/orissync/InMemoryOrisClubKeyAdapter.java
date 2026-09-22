package com.klabis.members.infrastructure.orissync;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * The club key does not survive a restart (design.md D9, Risks): held only in a
 * field of a running instance. A persistent encrypted store is the expected
 * follow-up, swapped in behind {@link OrisClubKeyPort} without touching callers.
 */
@SecondaryAdapter
@Component
class InMemoryOrisClubKeyAdapter implements OrisClubKeyPort, OrisClubKeyAccessor {

    @Value("klabis.oris.club.key")
    private volatile String clubKey;

    @Override
    public void store(String clubKey) {
        Assert.hasText(clubKey, "Club key must not be blank");
        this.clubKey = clubKey;
    }

    @Override
    public boolean isSet() {
        return clubKey != null;
    }

    @Override
    public void clear() {
        this.clubKey = null;
    }

    /**
     * Package-private by design: {@link OrisClubKeyPort} itself carries no getter, so
     * nothing outside {@code members.infrastructure.orissync} can read the key back
     * (design.md D9). Only {@link DefaultOrisClubMembers}, living in this same
     * package, may call it to attach the key to an outgoing ORIS request.
     */
    @Override
    public String currentKey() {
        return clubKey;
    }
}
