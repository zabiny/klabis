package club.klabis.users.application;

import club.klabis.members.MemberId;
import club.klabis.members.domain.events.MembershipSuspendedEvent;
import club.klabis.users.domain.ApplicationUser;
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
    public void suspendUserForMember(MemberId memberId) {
        ApplicationUser userForCreatedMember = applicationUsersRepository.findByMemberId(memberId)
                .orElseThrow(() -> ApplicationUserNotFound.forMemberId(memberId));

        LOG.info("Disabling Application user for suspended member id={}", memberId);

        userForCreatedMember.disable();
        applicationUsersRepository.save(userForCreatedMember);
    }

    // TODO: move into primary adapters
    @EventListener(MembershipSuspendedEvent.class)
    public void onMembershipSuspended(MembershipSuspendedEvent event) {
        MemberId memberId = event.getAggregate().getId();
        suspendUserForMember(memberId);
    }

}
