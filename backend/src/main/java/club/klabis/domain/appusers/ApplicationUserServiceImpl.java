package club.klabis.domain.appusers;

import club.klabis.domain.members.events.MemberCreatedEvent;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

@Service
@org.springframework.stereotype.Service
class ApplicationUserServiceImpl implements ApplicationUserService {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationUserServiceImpl.class);

    private final ApplicationUsersRepository repository;

    public ApplicationUserServiceImpl(ApplicationUsersRepository repository) {
        this.repository = repository;

        LOG.info("Adding user dpolach");
        ApplicationUser admin = ApplicationUser.newAppUser("dpolach", "{noop}secret");
        admin.linkWithGoogle("110875617296914468258");
        repository.save(admin);

        LOG.info("Adding user admin");
        admin = ApplicationUser.newAppUser("admin", "{noop}secret");
        repository.save(admin);

    }

    @Override
    public ApplicationUser getApplicationUserForMemberId(Integer memberId) {
        return repository.findByMemberId(memberId);
    }

    @EventListener(MemberCreatedEvent.class)
    public void onMemberCreated(MemberCreatedEvent event) {
        LOG.info("Creating Application user for new member %s (id=%s)".formatted(event.getAggregate().getRegistration(), event.getAggregate().getId()));
        ApplicationUser userForCreatedMember = ApplicationUser.newAppUser(event.getAggregate(), "{nop}password");
        repository.save(userForCreatedMember);
    }
}
