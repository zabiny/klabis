package club.klabis.shared.config.security;

public interface KlabisSecurityService {
    String BEAN_NAME = "klabisAuthorizationService";

    boolean canEditMemberData(int dataMemberId);

    boolean hasGrant(ApplicationGrant grant);
}
