package club.klabis.application.users;

import club.klabis.domain.members.Member;
import club.klabis.domain.members.events.MembershipSuspendedEvent;
import club.klabis.domain.users.ApplicationUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSuspendedUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(UserSuspendedUseCase.class);

    private final ApplicationUsersRepository applicationUsersRepository;

    public UserSuspendedUseCase(ApplicationUsersRepository applicationUsersRepository) {
        this.applicationUsersRepository = applicationUsersRepository;
    }

    @Transactional
    public void suspendUserForMember(Member.Id memberId) {
        ApplicationUser userForCreatedMember = applicationUsersRepository.findByMemberId(memberId)
                .orElseThrow(() -> ApplicationUserNotFound.forMemberId(memberId));

        LOG.info("Disabling Application user for suspended member id={}", memberId);

        userForCreatedMember.disable();
        applicationUsersRepository.save(userForCreatedMember);
    }

    // TODO: move into primary adapters
    @EventListener(MembershipSuspendedEvent.class)
    public void onMembershipSuspended(MembershipSuspendedEvent event) {
        Member.Id memberId = event.getAggregate().getId();
        suspendUserForMember(memberId);
    }

}
