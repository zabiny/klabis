package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.members.CurrentUserData;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import org.springframework.security.access.AccessDeniedException;

final class GuardianListAccess {

    private GuardianListAccess() {
    }

    // A minor's guardians are part of the data a holder of profile editing sees; every minor of the group has
    // the same guardians, so holding it over any one of them is enough.
    static boolean permits(AuthorizationEvaluator authorizationEvaluator, CurrentUserData user, LegalGuardianGroup group) {
        return authorizationEvaluator.has(Authority.MEMBERS_MANAGE)
               || user.isMemberOf(memberId -> group.hasMember(memberId.toUserId()))
               || group.getMembers().stream().anyMatch(minor -> authorizationEvaluator.has(
                Authority.MEMBERS_EDIT_PROFILE, TargetRef.member(minor.memberId().uuid())));
    }

    static void require(AuthorizationEvaluator authorizationEvaluator, CurrentUserData user, LegalGuardianGroup group) {
        if (!permits(authorizationEvaluator, user, group)) {
            throw new AccessDeniedException(
                    "Access to legal guardians requires MEMBERS:MANAGE authority, being a minor of the group"
                    + " or holding MEMBERS:EDIT_PROFILE over a minor of the group");
        }
    }
}
