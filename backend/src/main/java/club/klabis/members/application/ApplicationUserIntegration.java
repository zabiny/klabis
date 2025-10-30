package club.klabis.members.application;

import club.klabis.members.domain.Member;
import club.klabis.members.domain.events.MemberCreatedEvent;
import club.klabis.members.domain.events.MembershipSuspendedEvent;
import club.klabis.users.application.CreateApplicationUserUseCase;
import club.klabis.users.application.UserSuspendedUseCase;
import club.klabis.users.domain.ApplicationUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ApplicationUserIntegration {

    private final MembersRepository membersRepository;
    private final CreateApplicationUserUseCase createApplicationUserUseCase;
    private final UserSuspendedUseCase userSuspendedUseCase;

    private static final Logger LOG = LoggerFactory.getLogger(CreateApplicationUserUseCase.class);

    ApplicationUserIntegration(MembersRepository membersRepository, CreateApplicationUserUseCase createApplicationUserUseCase, UserSuspendedUseCase userSuspendedUseCase) {
        this.membersRepository = membersRepository;
        this.createApplicationUserUseCase = createApplicationUserUseCase;
        this.userSuspendedUseCase = userSuspendedUseCase;
    }

    @Transactional
    @EventListener(MemberCreatedEvent.class)
    public void onMemberCreated(MemberCreatedEvent event) {
        Member member = membersRepository.findById(event.getAggregate().getId()).orElseThrow();

        LOG.info("Creating Application user for new member %s (id=%s)".formatted(member.getRegistration(),
                member.getId()));

        ApplicationUser userForCreatedMember = createApplicationUserUseCase.createApplicationUser(ApplicationUser.UserName.of(
                member.getRegistration()
                        .toRegistrationId()), "{noop}password");

        member.linkWithApplicationUser(userForCreatedMember.getId());

        membersRepository.save(member);
    }

    @Transactional
    @EventListener(MembershipSuspendedEvent.class)
    public void onMembershipSuspended(MembershipSuspendedEvent event) {
        event.getAggregate().getAppUserId().ifPresentOrElse(appUserId -> {
            userSuspendedUseCase.suspendUserForMember(appUserId);
        }, () -> {
            LOG.warn("No application user ID linked to member %s".formatted(event.getAggregate().getRegistration()));
        });
    }


}
