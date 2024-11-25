package club.klabis.domain.appusers;

import club.klabis.domain.members.Member;
import club.klabis.domain.members.RegistrationNumber;

import java.util.Collection;

public interface ApplicationUserService {
    ApplicationUser getApplicationUserForMemberId(Member.Id memberId);

    void setGlobalGrants(Member.Id memberId, Collection<ApplicationGrant> globalGrants);

    void linkWithGoogleId(RegistrationNumber registrationNumber, String googleId);
}
