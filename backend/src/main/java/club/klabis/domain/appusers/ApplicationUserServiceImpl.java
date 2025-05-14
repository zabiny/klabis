package club.klabis.domain.appusers;

import club.klabis.adapters.api.KlabisApplicationUserDetailsService;
import club.klabis.domain.members.Member;
import club.klabis.domain.members.RegistrationNumber;
import club.klabis.domain.members.events.MemberCreatedEvent;
import club.klabis.domain.members.events.MembershipSuspendedEvent;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
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

    @EventListener(MemberCreatedEvent.class)
    public void onMemberCreated(MemberCreatedEvent event) {
        LOG.info("Creating Application user for new member %s (id=%s)".formatted(event.getAggregate().getRegistration(),
                event.getAggregate().getId()));
        ApplicationUser userForCreatedMember = ApplicationUser.newAppUser(event.getAggregate(), "{noop}password");
        repository.save(userForCreatedMember);
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

    @Transactional
    @Override
    public void setGlobalGrants(Member.Id memberId, Collection<ApplicationGrant> globalGrants) {
        ApplicationUser memberAppUser = getApplicationUserForMemberId(memberId);
        memberAppUser.setGlobalGrants(globalGrants);
        repository.save(memberAppUser);
    }

    @Override
    public void linkWithGoogleId(RegistrationNumber registrationNumber, String googleId) {
        ApplicationUser memberAppUser = repository.findByUserName(registrationNumber.toRegistrationId()).orElseThrow();
        memberAppUser.linkWithGoogle(googleId);
        repository.save(memberAppUser);
    }

    @Override
    public Optional<ApplicationUser> findByGoogleId(String googleIdSubject) {
        return repository.findByGoogleSubject(googleIdSubject);
    }

}
