package com.klabis.common.users.application;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/**
 * Checks whether an e-mail address may receive an activation link for a member's account.
 *
 * <p>{@code common} owns this port but has no notion of members or guardians; {@code members}
 * implements it, since only that module knows a member's own and guardian's e-mail (design D2).
 */
@SecondaryPort
public interface ActivationContactVerifier {

    /**
     * @param registrationNumber the account's username
     * @param email              the e-mail address supplied by the caller
     * @return {@code true} when {@code email} matches the member's own or guardian's e-mail
     * (trimmed, case-insensitive); {@code false} for an unknown registration number, a member
     * without any e-mail, or any non-matching address
     */
    boolean isActivationContact(String registrationNumber, String email);
}
