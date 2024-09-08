package club.klabis.appusers.domain;

import club.klabis.members.events.MemberCreatedEvent;
import org.jmolecules.architecture.onion.simplified.ApplicationRing;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@ApplicationRing
@Service
@org.springframework.stereotype.Service
class ApplicationUserServiceImpl implements ApplicationUserService {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationUserServiceImpl.class);

    private final ApplicationUsersRepository repository;

    public ApplicationUserServiceImpl(ApplicationUsersRepository repository) {
        this.repository = repository;
    }

    @Override
    public ApplicationUser getApplicationUserForMemberId(Integer memberId) {
        return repository.findByMemberId(memberId);
    }

    @EventListener(MemberCreatedEvent.class)
    public void onMemberCreated(MemberCreatedEvent event) {
        LOG.info("Creating Application user for new member %s (id=%s)".formatted(event.getAggregate().getRegistration(), event.getAggregate().getId()));
        ApplicationUser userForCreatedMember = ApplicationUser.newAppUser(event.getAggregate(), "{noop}password");
        repository.save(userForCreatedMember);
    }

    @Transactional
    @Override
    public void setGlobalGrants(Integer memberId, Collection<ApplicationGrant> globalGrants) {
        ApplicationUser memberAppUser = getApplicationUserForMemberId(memberId);
        memberAppUser.setGlobalGrants(globalGrants);
        repository.save(memberAppUser);
    }

}