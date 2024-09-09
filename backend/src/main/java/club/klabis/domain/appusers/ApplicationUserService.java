package club.klabis.domain.appusers;

import club.klabis.domain.members.RegistrationNumber;

import java.util.Collection;

public interface ApplicationUserService {
    ApplicationUser getApplicationUserForMemberId(Integer memberId);

    void setGlobalGrants(Integer memberId, Collection<ApplicationGrant> globalGrants);

    void linkWithGoogleId(RegistrationNumber registrationNumber, String googleId);
}
