package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * What the legal guardians of a minor offer towards a member's completeness: whether there is any
 * guardian at all and whether any of them has an e-mail and any has a telephone. E-mail and
 * telephone may come from different guardians.
 */
@ValueObject
public record GuardianContacts(boolean hasGuardian, boolean hasEmail, boolean hasPhone) {

    public static final GuardianContacts NONE = new GuardianContacts(false, false, false);

    public GuardianContacts and(GuardianContacts other) {
        return new GuardianContacts(hasGuardian || other.hasGuardian, hasEmail || other.hasEmail,
                hasPhone || other.hasPhone);
    }
}
