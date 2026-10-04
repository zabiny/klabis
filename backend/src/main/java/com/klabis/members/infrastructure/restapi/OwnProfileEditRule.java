package com.klabis.members.infrastructure.restapi;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.users.Authority;
import com.klabis.members.domain.PersonalInformation;

import java.time.LocalDate;

/**
 * A minor's data is maintained by guardians and administrators, not by the child. Owner access
 * (x-klabis-owner-visible) knows nothing about age, so this rule narrows it for minors; it is shared by
 * the "Upravit" affordance and the update itself so the two cannot disagree. Age is evaluated against
 * today, which re-enables self-editing on the 18th birthday without any intervention.
 */
final class OwnProfileEditRule {

    private OwnProfileEditRule() {
    }

    static boolean isForbidden(LocalDate dateOfBirth, AuthorizationEvaluator authorizationEvaluator) {
        return PersonalInformation.isMinor(dateOfBirth) && !authorizationEvaluator.has(Authority.MEMBERS_MANAGE);
    }
}
