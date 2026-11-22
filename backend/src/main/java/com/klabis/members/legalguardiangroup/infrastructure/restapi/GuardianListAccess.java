package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

final class GuardianListAccess {

    private static final List<Authority> PROFILE_AUTHORITIES =
            List.of(Authority.MEMBERS_EDIT_PROFILE, Authority.MEMBERS_VIEW_PROFILE);

    private GuardianListAccess() {
    }

    // A minor's guardians are part of the data a holder of profile viewing or editing sees; every minor of the group
    // has the same guardians, so holding it over any one of them is enough. This covers the minor too, who holds
    // viewing over themself.
    static boolean permits(AuthorizationEvaluator authorizationEvaluator, LegalGuardianGroup group) {
        return authorizationEvaluator.has(Authority.MEMBERS_MANAGE)
               || group.getMembers().stream().anyMatch(minor -> authorizationEvaluator.isAllowed(
                PROFILE_AUTHORITIES, TargetRef.member(minor.memberId().uuid()), false));
    }

    static void require(AuthorizationEvaluator authorizationEvaluator, LegalGuardianGroup group) {
        if (!permits(authorizationEvaluator, group)) {
            throw new AccessDeniedException(
                    "Access to legal guardians requires MEMBERS:MANAGE authority"
                    + " or holding MEMBERS:EDIT_PROFILE or MEMBERS:VIEW_PROFILE over a minor of the group");
        }
    }
}
