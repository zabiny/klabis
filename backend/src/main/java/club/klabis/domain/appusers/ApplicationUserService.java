package club.klabis.domain.appusers;

import java.util.Collection;

public interface ApplicationUserService {
    ApplicationUser getApplicationUserForMemberId(Integer memberId);

    void setGlobalGrants(Integer memberId, Collection<ApplicationGrant> globalGrants);
}
