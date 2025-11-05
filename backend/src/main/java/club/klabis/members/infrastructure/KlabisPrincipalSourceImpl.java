package club.klabis.members.infrastructure;

import club.klabis.members.MemberId;
import club.klabis.members.application.MembersRepository;
import club.klabis.members.domain.Member;
import club.klabis.shared.config.restapi.KlabisPrincipal;
import club.klabis.shared.config.restapi.KlabisPrincipalSource;
import club.klabis.users.application.ApplicationUsersRepository;
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
                    MemberId memberId = membersRepository.findMemberByAppUserId(appUser.getId())
                            .map(Member::getId)
                            .orElse(null);
                    return new KlabisPrincipal(appUser.getId(),
                            memberId,
                            appUser.getGlobalGrants());
                });
    }

}
