package club.klabis.domain.appusers;

import club.klabis.domain.members.Member;
import club.klabis.domain.members.RegistrationNumber;

public interface ApplicationUserService {
    ApplicationUser getApplicationUserForMemberId(Member.Id memberId);

}
