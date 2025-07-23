package club.klabis.domain.users;

import club.klabis.domain.members.Member;

public interface ApplicationUserService {
    ApplicationUser getApplicationUserForMemberId(Member.Id memberId);

}
