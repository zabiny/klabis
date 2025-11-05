package club.klabis.members.infrastructure;

import club.klabis.members.MemberId;
import club.klabis.members.application.MembersRepository;
import club.klabis.members.domain.Member;
import club.klabis.shared.config.restapi.KlabisPrincipal;
import club.klabis.shared.config.restapi.KlabisPrincipalSource;
import club.klabis.users.application.ApplicationUsersRepository;
import club.klabis.users.domain.ApplicationUser;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
class KlabisPrincipalSourceImpl implements KlabisPrincipalSource {

    private final MembersRepository membersRepository;
    private final ApplicationUsersRepository applicationUserRepository;

    public KlabisPrincipalSourceImpl(MembersRepository membersRepository, ApplicationUsersRepository applicationUserRepository) {
        this.membersRepository = membersRepository;
        this.applicationUserRepository = applicationUserRepository;
    }


    @Override
    public Optional<KlabisPrincipal> getPrincipalForUserName(String username) {
        return applicationUserRepository.findByUserNameValue(username)
                .map(appUser -> {
                    Member member = membersRepository.findMemberByAppUserId(appUser.getId()).orElse(null);
                    return createPrincipal(appUser, member);
                });
    }

    private KlabisPrincipal createPrincipal(ApplicationUser applicationUser, @Nullable Member member) {
        MemberId memberId = null;
        String firstName = null;
        String lastName = null;
        if (member != null) {
            memberId = member.getId();
            firstName = member.getFirstName();
            lastName = member.getLastName();
        }

        return new KlabisPrincipal(applicationUser.getId(),
                memberId,
                applicationUser.getUsername().value(),
                firstName, lastName,
                applicationUser.getGlobalGrants());
    }

    ;

}
