package com.klabis.common.users.application;

import com.klabis.common.users.UserId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

/**
 * Checks whether an e-mail address may receive an activation link for a user's account.
 *
 * <p>{@code common} owns this port but has no notion of members or guardians; {@code members}
 * implements it, since only that module knows who the user is and which e-mail is their own (design D9).
 */
@SecondaryPort
public interface ActivationContactVerifier {

    /**
     * The account is identified by {@link UserId} rather than by username: a non-member guardian logs in
     * with a generated {@code EXTnnnn} number, and a guardian promoted to member keeps it (design D5).
     *
     * @return {@code true} when {@code email} is the user's own e-mail (trimmed, case-insensitive);
     * {@code false} for an unknown user, a minor, a user without an e-mail or a non-matching address
     */
    boolean isActivationContact(UserId userId, String email);
}
