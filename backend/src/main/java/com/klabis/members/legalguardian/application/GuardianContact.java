package com.klabis.members.legalguardian.application;

import com.klabis.common.users.UserId;

/**
 * Contact details of a legal guardian, wherever the guardian's data live: a member's own details
 * or the profile of a non-member legal guardian. A member may have no e-mail or phone.
 */
public record GuardianContact(UserId userId, String firstName, String lastName, String email, String phone,
                              GuardianKind kind) {
}
