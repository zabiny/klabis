package club.klabis.domain.users;

import club.klabis.application.users.ApplicationUserNotFound;
import club.klabis.application.users.ApplicationUsersRepository;
import club.klabis.domain.members.Member;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Service
@org.springframework.stereotype.Service
class ApplicationUserServiceImpl implements ApplicationUserService, KlabisApplicationUserDetailsService {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationUserServiceImpl.class);

    private final ApplicationUsersRepository repository;

    public ApplicationUserServiceImpl(ApplicationUsersRepository repository) {
        this.repository = repository;
    }

    @Override
    public ApplicationUser getApplicationUserForMemberId(Member.Id memberId) {
        return repository.findByMemberId(memberId).orElseThrow(() -> ApplicationUserNotFound.forMemberId(memberId));
    }

    @Override
    public Optional<ApplicationUser> getApplicationUserForUsername(String username) {
        return repository.findByUserName(username);
    }


}
