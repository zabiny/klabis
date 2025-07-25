package club.klabis.shared.config.security;

import club.klabis.shared.config.restapi.KlabisUserAuthentication;
import club.klabis.users.domain.ApplicationGrant;
import club.klabis.users.domain.ApplicationUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

@Component("klabisAuthorizationService")
public class KlabisSecurityService {

    private final Logger LOG = LoggerFactory.getLogger(KlabisSecurityService.class);

    private Optional<ApplicationUser> getPrincipal() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof KlabisUserAuthentication typedAuth) {
            return Optional.of(typedAuth).map(KlabisUserAuthentication::getPrincipal);
        } else {
            return Optional.empty();
        }
    }

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

    public boolean hasGrant(ApplicationGrant grant) {
        return getPrincipal().stream().anyMatch(hasAppGrant(grant));
    }

    private Predicate<? super ApplicationUser> hasAppGrant(ApplicationGrant grant) {
        return user -> user.getGlobalGrants().contains(grant);
    }

}
