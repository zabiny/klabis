package com.klabis.common.authorization;

import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.groups.domain.MemberGroup;
import com.klabis.common.users.Authority;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Turns groups into the grants their owners hold: what each group delegates, over each of its members.
 * Shared by the {@link RelationshipSource}s of every group type so they agree on the shape of a delegation.
 */
public final class DelegatedGrants {

    private DelegatedGrants() {
    }

    /**
     * Ownership of the groups themselves, for the owners of those groups. Held by every kind of group owner
     * (free group owner, trainer, guardian), never granted by hand or delegated.
     */
    private static Map<Authority, Set<TargetRef>> ownershipOfIds(Collection<UUID> ownedGroupIds) {
        if (ownedGroupIds.isEmpty()) {
            return Map.of();
        }
        return Map.of(Authority.GROUPS_OWNER,
                ownedGroupIds.stream().map(TargetRef::group).collect(Collectors.toUnmodifiableSet()));
    }

    /**
     * What the owner of {@code ownedGroups} holds: what each delegates over its members, plus ownership of the
     * groups themselves.
     */
    public static <ID, M> Map<Authority, Set<TargetRef>> of(Collection<? extends MemberGroup<?, ID, M>> ownedGroups,
                                                           Function<ID, UUID> groupIdToUuid,
                                                           Function<M, UUID> memberIdToUuid) {
        Map<Authority, Set<TargetRef>> grants = new EnumMap<>(Authority.class);
        grants.putAll(overMembersOf(ownedGroups, memberIdToUuid));
        grants.putAll(ownershipOf(ownedGroups, groupIdToUuid));
        return grants;
    }

    /**
     * Ownership of the groups themselves, for the owners of those groups. Held by every kind of group owner
     * (free group owner, trainer, guardian), never granted by hand or delegated.
     */
    public static <ID> Map<Authority, Set<TargetRef>> ownershipOf(Collection<? extends MemberGroup<?, ID, ?>> ownedGroups,
                                                                 Function<ID, UUID> groupIdToUuid) {
        if (ownedGroups.isEmpty()) {
            return Map.of();
        }
        return Map.of(Authority.GROUPS_OWNER,
                ownedGroups.stream()
                        .map(group -> TargetRef.group(groupIdToUuid.apply(group.getId())))
                        .collect(Collectors.toUnmodifiableSet()));
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
