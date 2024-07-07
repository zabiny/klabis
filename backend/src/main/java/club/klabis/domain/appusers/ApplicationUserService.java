package club.klabis.domain.appusers;

public interface ApplicationUserService {
    ApplicationUser getApplicationUserForMemberId(Integer memberId);
}
