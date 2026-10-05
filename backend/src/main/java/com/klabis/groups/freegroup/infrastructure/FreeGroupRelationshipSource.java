package com.klabis.groups.freegroup.infrastructure;

import com.klabis.common.authorization.DelegatedGrants;
import com.klabis.common.authorization.RelationshipSource;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.groups.freegroup.domain.FreeGroupFilter;
import com.klabis.groups.freegroup.domain.FreeGroupRepository;
import com.klabis.members.MemberId;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Owners of a free group hold what the group delegates over its members. Members and invitees gain nothing, and
 * the grant follows membership: it disappears with the member leaving or the owner stepping down.
 */
@Component
class FreeGroupRelationshipSource implements RelationshipSource {

    private final FreeGroupRepository groups;

    FreeGroupRelationshipSource(FreeGroupRepository groups) {
        this.groups = groups;
    }

    @Override
    public Map<Authority, Set<TargetRef>> grantsOf(UserId userId) {
        MemberId owner = MemberId.fromUserId(userId);
        return DelegatedGrants.overMembersOf(groups.findAll(FreeGroupFilter.all().withOwnerIs(owner)), MemberId::uuid);
    }
}
