package club.klabis.shared.config.security;

import club.klabis.members.MemberId;
import club.klabis.shared.config.restapi.KlabisPrincipal;

import java.util.Optional;

public interface KlabisSecurityService {
    String BEAN_NAME = "klabisAuthorizationService";

    Optional<KlabisPrincipal> getPrincipal();

    boolean canEditMemberData(MemberId dataMemberId);

    boolean hasGrant(ApplicationGrant grant);
}
