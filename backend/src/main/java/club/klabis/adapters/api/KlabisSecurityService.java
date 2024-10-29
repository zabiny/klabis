package club.klabis.adapters.api;

import club.klabis.domain.appusers.ApplicationGrant;
import club.klabis.domain.appusers.ApplicationUser;
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
        int authenticatedMemberId = getPrincipal().flatMap(ApplicationUser::getMemberId).orElse(-1);
        boolean canEditMemberData = dataMemberId == authenticatedMemberId;

        LOG.trace("Member {} attempt to edit data of member {} - {}", authenticatedMemberId, dataMemberId, canEditMemberData);

        return canEditMemberData;
    }

    public boolean hasGrant(ApplicationGrant grant) {
        return getPrincipal().stream().anyMatch(hasAppGrant(grant));
    }

    private Predicate<? super ApplicationUser> hasAppGrant(ApplicationGrant grant) {
        return user -> user.getGlobalGrants().contains(grant);
    }

}
