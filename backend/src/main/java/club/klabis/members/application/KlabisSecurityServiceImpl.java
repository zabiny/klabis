package club.klabis.members.application;

import club.klabis.members.MemberId;
import club.klabis.shared.config.restapi.KlabisPrincipal;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.KlabisSecurityService;
import club.klabis.users.domain.ApplicationUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component(KlabisSecurityService.BEAN_NAME)
public class KlabisSecurityServiceImpl implements KlabisSecurityService {

    private static final Logger LOG = LoggerFactory.getLogger(KlabisSecurityServiceImpl.class);

    private Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    public Optional<KlabisPrincipal> getPrincipal() {
        return getAuthentication()
                .flatMap(KlabisSecurityServiceImpl::getPrincipal);
    }

    private static Optional<KlabisPrincipal> getPrincipal(Authentication authentication) {
        if (authentication == null) return Optional.empty();

        if (authentication.getPrincipal() instanceof KlabisPrincipal typedPrincipal) {
            return Optional.of(typedPrincipal);
        } else {
            LOG.warn("KlabisPrincipal was found in authentication context {}", authentication);
        }

        return Optional.empty();
    }

    private Optional<MemberId> getPrincipalMemberId() {
        return getPrincipal()
                .map(KlabisPrincipal::memberId);
    }

    public boolean canEditMemberData(int dataMemberId) {
        return canEditMemberData(new MemberId(dataMemberId));
    }

    @Override
    public Optional<MemberId> getAuthenticatedMemberId() {
        return getPrincipal().map(KlabisPrincipal::memberId);
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
