package club.klabis.application.users;

import club.klabis.domain.users.ApplicationGrant;
import club.klabis.domain.users.ApplicationUser;
import club.klabis.domain.members.Member;
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
    public void setGlobalGrants(Member.Id memberId, Collection<ApplicationGrant> globalGrants) {
        ApplicationUser memberAppUser = applicationUsersRepository.findByMemberId(memberId)
                .orElseThrow(() -> ApplicationUserNotFound.forMemberId(memberId));
        memberAppUser.setGlobalGrants(globalGrants);
        applicationUsersRepository.save(memberAppUser);
    }
}
