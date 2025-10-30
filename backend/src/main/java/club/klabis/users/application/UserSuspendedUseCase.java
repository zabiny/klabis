package club.klabis.users.application;

import club.klabis.shared.config.ddd.UseCase;
import club.klabis.users.domain.ApplicationUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class UserSuspendedUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(UserSuspendedUseCase.class);

    private final ApplicationUsersRepository applicationUsersRepository;

    public UserSuspendedUseCase(ApplicationUsersRepository applicationUsersRepository) {
        this.applicationUsersRepository = applicationUsersRepository;
    }

    @Transactional
    public void suspendUserForMember(ApplicationUser.Id appUserId) {
        ApplicationUser userForCreatedMember = applicationUsersRepository.findById(appUserId)
                .orElseThrow(() -> ApplicationUserNotFound.forUserId(appUserId));

        LOG.info("Disabling Application user id={}", appUserId);

        userForCreatedMember.disable();
        applicationUsersRepository.save(userForCreatedMember);
    }

}
