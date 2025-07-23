package club.klabis.application.users;

import club.klabis.domain.users.ApplicationUser;
import club.klabis.domain.members.Member;
import club.klabis.domain.members.events.MemberCreatedEvent;
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
        ApplicationUser userForCreatedMember = ApplicationUser.newAppUser(member, "{noop}password");
        applicationUsersRepository.save(userForCreatedMember);
    }

    @EventListener(MemberCreatedEvent.class)
    public void onMemberCreated(MemberCreatedEvent event) {
        createForMember(event.getAggregate());
    }

}
