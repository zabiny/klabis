package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.members.CurrentUserData;
import com.klabis.members.domain.PersonalInformation;
import org.springframework.security.core.Authentication;

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

    static boolean isForbidden(LocalDate dateOfBirth, Authentication authentication) {
        boolean canManageMembers = CurrentUserData.from(authentication)
                .map(user -> user.hasAuthority(Authority.MEMBERS_MANAGE))
                .orElse(false);
        return PersonalInformation.isMinor(dateOfBirth) && !canManageMembers;
    }
}
