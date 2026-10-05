package com.klabis.groups.freegroup.infrastructure;

import com.klabis.common.authorization.RelationshipSource;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.FreeGroupFilter;
import com.klabis.groups.freegroup.domain.FreeGroupRepository;
import com.klabis.members.MemberId;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashSet;
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
        MemberId user = MemberId.fromUserId(userId);
        Map<Authority, Set<TargetRef>> grants = new EnumMap<>(Authority.class);
        for (FreeGroup group : groups.findAll(FreeGroupFilter.all().withOwnerOrMemberIs(user))) {
            if (!group.isOwner(user)) {
                continue;
            }
            Set<TargetRef> members = membersOf(group);
            if (members.isEmpty()) {
                continue;
            }
            for (Authority authority : group.delegatedAuthorities()) {
                grants.computeIfAbsent(authority, key -> new HashSet<>()).addAll(members);
            }
        }
        return grants;
    }

    private static Set<TargetRef> membersOf(FreeGroup group) {
        Set<TargetRef> members = new HashSet<>();
        for (GroupMembership<MemberId> member : group.getMembers()) {
            members.add(TargetRef.member(member.memberId().uuid()));
        }
        return members;
    }
}
