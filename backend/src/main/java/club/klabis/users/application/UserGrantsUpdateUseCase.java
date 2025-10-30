package club.klabis.users.application;

import club.klabis.shared.config.ddd.UseCase;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.users.domain.ApplicationUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@UseCase
public class UserGrantsUpdateUseCase {

    private final ApplicationUsersRepository applicationUsersRepository;

    public UserGrantsUpdateUseCase(ApplicationUsersRepository applicationUsersRepository) {
        this.applicationUsersRepository = applicationUsersRepository;
    }

    @Transactional
    public void setGlobalGrants(ApplicationUser.Id userId, Collection<ApplicationGrant> globalGrants) {
        ApplicationUser memberAppUser = applicationUsersRepository.findById(userId)
                .orElseThrow(() -> ApplicationUserNotFound.forUserId(userId));
        memberAppUser.setGlobalGrants(globalGrants);
        applicationUsersRepository.save(memberAppUser);
    }
}
