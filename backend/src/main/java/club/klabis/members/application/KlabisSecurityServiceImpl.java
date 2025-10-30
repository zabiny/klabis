package club.klabis.members.application;

import club.klabis.members.MemberId;
import club.klabis.shared.config.restapi.KlabisPrincipal;
import club.klabis.shared.config.restapi.KlabisUserAuthentication;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import club.klabis.users.domain.ApplicationUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component(KlabisSecurityService.BEAN_NAME)
public class KlabisSecurityServiceImpl implements KlabisSecurityService {

    private final Logger LOG = LoggerFactory.getLogger(KlabisSecurityServiceImpl.class);

    private Optional<KlabisPrincipal> getPrincipal() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof KlabisUserAuthentication typedAuth) {
            return Optional.of(typedAuth).map(KlabisUserAuthentication::getPrincipal);
            // TODO: doesn't work when logged in "locally" - it gets Google's OAuth2 authentication instead of klabis user...
        } else {
            return Optional.empty();
        }
    }

    private Optional<MemberId> getPrincipalMemberId() {
        return getPrincipal()
                .map(KlabisPrincipal::memberId);
    }

    public boolean canEditMemberData(int dataMemberId) {
        return canEditMemberData(new MemberId(dataMemberId));
    }

    @Override
    public boolean canEditMemberData(MemberId dataMemberId) {
        boolean canEditMemberData = getPrincipalMemberId()
                .map(dataMemberId::equals)
                .orElse(false);

        LOG.trace("Application user with ID {} attempt to edit data of member {} - {}",
                getPrincipal()
                        .map(KlabisPrincipal::userId)
                        .map(ApplicationUser.Id::value)
                        .map(String::valueOf)
                        .orElse("no-user-logged-in"),
                dataMemberId,
                canEditMemberData);

        return canEditMemberData;
    }

    @Override
    public boolean hasGrant(ApplicationGrant grant) {
        return getPrincipal().stream().anyMatch(principal -> principal.hasAppGrant(grant));
    }

}
