package club.klabis.application.users;

import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.domain.appusers.ApplicationUserNotFound;
import club.klabis.domain.members.RegistrationNumber;
import org.springframework.stereotype.Service;

@Service
public class LinkWithSocialIdUseCase {

    private final ApplicationUsersRepository applicationUsersRepository;

    public LinkWithSocialIdUseCase(ApplicationUsersRepository applicationUsersRepository) {
        this.applicationUsersRepository = applicationUsersRepository;
    }

    public void linkWithGoogleId(RegistrationNumber registrationNumber, String googleId) {
        ApplicationUser memberAppUser = applicationUsersRepository.findByUserName(registrationNumber.toRegistrationId()).orElseThrow(() -> ApplicationUserNotFound.forRegistrationId(registrationNumber));
        memberAppUser.linkWithGoogle(googleId);
        applicationUsersRepository.save(memberAppUser);
    }


}
