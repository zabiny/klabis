package com.klabis.common.authorization;

import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.groups.domain.MemberGroup;
import com.klabis.common.users.Authority;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Turns groups into the grants their owners hold: what each group delegates, over each of its members.
 * Shared by the {@link RelationshipSource}s of every group type so they agree on the shape of a delegation.
 */
public final class DelegatedGrants {

    private DelegatedGrants() {
    }

    /**
     * The union over {@code ownedGroups} of what each delegates over its members. A group without members or
     * delegating nothing contributes nothing.
     */
    public static <M> Map<Authority, Set<TargetRef>> overMembersOf(Iterable<? extends MemberGroup<?, ?, M>> ownedGroups,
                                                                  Function<M, UUID> memberIdToUuid) {
        Map<Authority, Set<TargetRef>> grants = new EnumMap<>(Authority.class);
        for (MemberGroup<?, ?, M> group : ownedGroups) {
            Set<TargetRef> members = new HashSet<>();
            for (GroupMembership<M> membership : group.getMembers()) {
                members.add(TargetRef.member(memberIdToUuid.apply(membership.memberId())));
            }
            if (members.isEmpty()) {
                continue;
            }
            for (Authority authority : group.delegatedAuthorities()) {
                grants.computeIfAbsent(authority, key -> new HashSet<>()).addAll(members);
            }
        }
        return grants;
    }
}
