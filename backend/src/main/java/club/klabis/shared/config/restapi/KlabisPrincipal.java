package club.klabis.shared.config.restapi;

import club.klabis.members.MemberId;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.users.domain.ApplicationUser;

import java.util.Set;

public record KlabisPrincipal(ApplicationUser.Id userId, MemberId memberId, Set<ApplicationGrant> globalGrants) {

    public boolean hasAppGrant(ApplicationGrant grant) {
        return globalGrants().contains(grant);
    }


}
