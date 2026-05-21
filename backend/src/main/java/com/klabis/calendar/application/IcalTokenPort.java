package com.klabis.calendar.application;

import com.klabis.common.users.UserId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.time.Instant;
import java.util.Optional;

@PrimaryPort
public interface IcalTokenPort {

    /**
     * Returns the token state for the user: the lookup prefix (first 8 chars of raw token)
     * and {@code lastSetAt}, or empty if no token exists yet.
     * <p>
     * The lookup prefix is NOT the raw token — it is the non-secret index prefix stored in the DB.
     * It is safe to expose it but conveys no information about the full token.
     */
    Optional<TokenState> getTokenState(UserId userId);

    record TokenState(String tokenLookup, Instant lastSetAt) {}

    /**
     * Generates a new token for the user, or rotates the existing one if it already exists.
     * The previous subscribe URL immediately stops working upon rotation.
     *
     * @return the raw token and the persisted {@code lastSetAt} timestamp (both shown/used once)
     */
    GenerateResult generateOrRotate(UserId userId);

    record GenerateResult(String rawToken, Instant lastSetAt) {}

    /**
     * Validates a raw token from the iCal feed URL query param.
     * Looks up by the non-secret prefix (indexed), then verifies via bcrypt.
     *
     * @return the owner's UserId when valid, empty otherwise
     */
    Optional<UserId> validate(String rawToken);
}
