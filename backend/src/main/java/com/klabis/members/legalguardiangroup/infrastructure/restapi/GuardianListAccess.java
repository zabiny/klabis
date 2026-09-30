package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.members.CurrentUserData;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import org.springframework.security.access.AccessDeniedException;

final class GuardianListAccess {

    private GuardianListAccess() {
    }

    static boolean permits(CurrentUserData user, LegalGuardianGroup group) {
        return user.hasAuthority(Authority.MEMBERS_MANAGE)
               || user.isMemberOf(memberId -> group.hasMember(memberId.toUserId()));
    }

    static void require(CurrentUserData user, LegalGuardianGroup group) {
        if (!permits(user, group)) {
            throw new AccessDeniedException(
                    "Access to legal guardians requires MEMBERS:MANAGE authority or being a minor of the group");
        }
    }
}
