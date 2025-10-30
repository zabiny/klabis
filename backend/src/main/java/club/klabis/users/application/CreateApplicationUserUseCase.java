package club.klabis.users.application;

import club.klabis.shared.config.ddd.UseCase;
import club.klabis.users.domain.ApplicationUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UseCase
public class CreateApplicationUserUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CreateApplicationUserUseCase.class);

    private final ApplicationUsersRepository applicationUsersRepository;

    public CreateApplicationUserUseCase(ApplicationUsersRepository applicationUsersRepository) {
        this.applicationUsersRepository = applicationUsersRepository;
    }

    public ApplicationUser createApplicationUser(ApplicationUser.UserName userName, String password) {
        LOG.debug("Create Application User {}", userName);
        ApplicationUser createdUser = ApplicationUser.newAppUser(userName, password);
        applicationUsersRepository.save(createdUser);
        return createdUser;
    }

}
