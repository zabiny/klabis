package com.klabis.finance.infrastructure;

import com.klabis.common.authorization.RelationshipSource;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * A member holds FINANCE:VIEW over their own account. Users without a member profile (non-member guardians,
 * the bootstrap admin) have no account and get no self-grant.
 */
@Component
class AccountOwnerRelationshipSource implements RelationshipSource {

    private final Members members;

    AccountOwnerRelationshipSource(Members members) {
        this.members = members;
    }

    @Override
    public Map<Authority, Set<TargetRef>> grantsOf(UserId userId) {
        if (!members.exists(MemberId.fromUserId(userId))) {
            return Map.of();
        }
        return Map.of(Authority.FINANCE_VIEW, Set.of(TargetRef.member(userId.uuid())));
    }
}
