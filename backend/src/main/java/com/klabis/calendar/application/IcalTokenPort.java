package com.klabis.calendar.application;

import com.klabis.common.users.UserId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Optional;

@PrimaryPort
public interface IcalTokenPort {

    /**
     * Generates a new token for the user. If a token already exists it is regenerated (overwritten).
     *
     * @return the raw token (shown to the user once — not stored)
     */
    String generate(UserId userId);

    /**
     * Regenerates (rotates) the token for the user, invalidating the previous URL.
     *
     * @return the new raw token
     * @throws com.klabis.common.exceptions.BusinessRuleViolationException if no token exists for the user yet
     */
    String regenerate(UserId userId);

    /**
     * Validates a raw token from the iCal feed URL query param.
     * Looks up by the non-secret prefix (indexed), then verifies via bcrypt.
     *
     * @return the owner's UserId when valid, empty otherwise
     */
    Optional<UserId> validate(String rawToken);
}
