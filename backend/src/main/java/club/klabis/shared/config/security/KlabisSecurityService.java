package club.klabis.shared.config.security;

import club.klabis.members.MemberId;

import java.util.Optional;

public interface KlabisSecurityService {
    String BEAN_NAME = "klabisAuthorizationService";

    /**
     * @return MemberId of authenticated user. If authenticated user is not a member (for example parent of some child member, admin, etc), returns empty optional same like when none user is authenticated.
     */
    Optional<MemberId> getAuthenticatedMemberId();

    /**
     * @param dataMemberId Member whom data are to be checked.
     * @return true if an authenticated user can edit data of a given memberId. Usually in situation when authenticated user is same as dataMemberId
     */
    boolean canEditMemberData(MemberId dataMemberId);

    /**
     * @param grant Grant which needs to be held by authenticated user to be authorized for operation
     * @return true if authenticated user hold given grant
     */
    boolean hasGrant(ApplicationGrant grant);
}
