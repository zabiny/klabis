package club.klabis.shared.config.security;

import club.klabis.members.MemberId;

public interface KlabisSecurityService {
    String BEAN_NAME = "klabisAuthorizationService";

    boolean canEditMemberData(MemberId dataMemberId);

    boolean hasGrant(ApplicationGrant grant);
}
