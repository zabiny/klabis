package club.klabis.users.application;

import club.klabis.members.domain.Member;
import club.klabis.members.domain.events.MemberCreatedEvent;
import club.klabis.users.domain.ApplicationUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class UserCreateUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(UserCreateUseCase.class);

    private final ApplicationUsersRepository applicationUsersRepository;

    public UserCreateUseCase(ApplicationUsersRepository applicationUsersRepository) {
        this.applicationUsersRepository = applicationUsersRepository;
    }

    public void createForMember(Member member) {
        LOG.info("Creating Application user for new member %s (id=%s)".formatted(member.getRegistration(),
                member.getId()));
        ApplicationUser userForCreatedMember = ApplicationUser.newAppUser(ApplicationUser.UserName.of(member.getRegistration()
                .toRegistrationId()), "{noop}password");
        applicationUsersRepository.save(userForCreatedMember);
    }

    // TODO: move into primary adapters
    @EventListener(MemberCreatedEvent.class)
    public void onMemberCreated(MemberCreatedEvent event) {
        createForMember(event.getAggregate());
    }

}
