package club.klabis.users.application;

import club.klabis.shared.config.ddd.UseCase;
import club.klabis.users.domain.ApplicationUser;

@UseCase
public class LinkWithSocialIdUseCase {

    private final ApplicationUsersRepository applicationUsersRepository;

    public LinkWithSocialIdUseCase(ApplicationUsersRepository applicationUsersRepository) {
        this.applicationUsersRepository = applicationUsersRepository;
    }

    public void linkWithGoogleId(ApplicationUser.Id appUserId, String googleId) {
        ApplicationUser memberAppUser = applicationUsersRepository.findById(appUserId)
                .orElseThrow(() -> ApplicationUserNotFound.forUserId(appUserId));
        memberAppUser.linkWithGoogle(googleId);
        applicationUsersRepository.save(memberAppUser);
    }


}
