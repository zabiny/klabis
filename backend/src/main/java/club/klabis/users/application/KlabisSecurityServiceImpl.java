package club.klabis.users.application;

import club.klabis.members.MemberId;
import club.klabis.members.application.MembersRepository;
import club.klabis.members.domain.Member;
import club.klabis.shared.config.restapi.KlabisUserAuthentication;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import club.klabis.users.domain.ApplicationUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

@Component(KlabisSecurityService.BEAN_NAME)
public class KlabisSecurityServiceImpl implements KlabisSecurityService {

    private final Logger LOG = LoggerFactory.getLogger(KlabisSecurityServiceImpl.class);

    private final MembersRepository membersRepository;

    public KlabisSecurityServiceImpl(MembersRepository membersRepository) {
        this.membersRepository = membersRepository;
    }

    private Optional<ApplicationUser> getPrincipal() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof KlabisUserAuthentication typedAuth) {
            return Optional.of(typedAuth).map(KlabisUserAuthentication::getPrincipal);
            // TODO: doesn't work when logged in "locally" - it gets Google's OAuth2 authentication instead of klabis user...
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean canEditMemberData(MemberId dataMemberId) {
        boolean canEditMemberData = getPrincipal().map(ApplicationUser::getId)
                .flatMap(membersRepository::findMemberByAppUserId)
                .map(Member::getId)
                .map(dataMemberId::equals)
                .orElse(false);

        LOG.trace("Application user with ID {} attempt to edit data of member {} - {}",
                getPrincipal()
                        .map(ApplicationUser::getId)
                        .map(ApplicationUser.Id::value)
                        .map(String::valueOf)
                        .orElse("no-user-logged-in"),
                dataMemberId,
                canEditMemberData);

        return canEditMemberData;
    }

    @Override
    public boolean hasGrant(ApplicationGrant grant) {
        return getPrincipal().stream().anyMatch(hasAppGrant(grant));
    }

    private Predicate<? super ApplicationUser> hasAppGrant(ApplicationGrant grant) {
        return user -> user.getGlobalGrants().contains(grant);
    }

}
