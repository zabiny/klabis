package com.klabis.groups;

import com.klabis.common.TargetGrant;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.users.Authority;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Member aaaaaaaa-... holding GROUPS:OWNER over group cccccccc-..., the ids used by the group controller tests.
 */
@Retention(RetentionPolicy.RUNTIME)
@WithKlabisMockUser(memberId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        targetGrants = @TargetGrant(authority = Authority.GROUPS_OWNER, type = TargetType.GROUP,
                ids = "cccccccc-cccc-cccc-cccc-cccccccccccc"))
public @interface WithGroupOwner {
}
