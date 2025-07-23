package club.klabis.domain.users;

import club.klabis.application.users.ApplicationUsersRepository;
import club.klabis.domain.members.Member;
import club.klabis.domain.members.events.MembershipSuspendedEvent;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

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

    @EventListener(MembershipSuspendedEvent.class)
    public void onMembershipSuspended(MembershipSuspendedEvent event) {
        Member.Id memberId = event.getAggregate().getId();
        LOG.info("Disabling Application user for suspended member %s (id=%s)".formatted(event.getAggregate()
                .getRegistration(), memberId));
        ApplicationUser userForCreatedMember = repository.findByMemberId(memberId)
                .orElseThrow(() -> ApplicationUserNotFound.forMemberId(memberId));
        userForCreatedMember.disable();
        repository.save(userForCreatedMember);
    }

}
