package club.klabis.users.application;

import club.klabis.members.MemberId;
import club.klabis.users.domain.ApplicationGrant;
import club.klabis.users.domain.ApplicationUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
public class UserGrantsUpdateUseCase {

    private final ApplicationUsersRepository applicationUsersRepository;

    public UserGrantsUpdateUseCase(ApplicationUsersRepository applicationUsersRepository) {
        this.applicationUsersRepository = applicationUsersRepository;
    }

    @Transactional
    public void setGlobalGrants(MemberId memberId, Collection<ApplicationGrant> globalGrants) {
        ApplicationUser memberAppUser = applicationUsersRepository.findByMemberId(memberId)
                .orElseThrow(() -> ApplicationUserNotFound.forMemberId(memberId));
        memberAppUser.setGlobalGrants(globalGrants);
        applicationUsersRepository.save(memberAppUser);
    }
}
