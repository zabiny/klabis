package club.klabis.users.application;

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

    private Optional<ApplicationUser> getPrincipal() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof KlabisUserAuthentication typedAuth) {
            return Optional.of(typedAuth).map(KlabisUserAuthentication::getPrincipal);
            // TODO: doesn't work when logged in "locally" - it gets Google's OAuth2 authentication instead of klabis user...
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean canEditMemberData(int dataMemberId) {
        boolean canEditMemberData = getPrincipal().flatMap(ApplicationUser::getMemberId)
                .map(authMemberId -> authMemberId.value() == dataMemberId)
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
