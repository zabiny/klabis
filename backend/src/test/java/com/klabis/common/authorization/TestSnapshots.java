package com.klabis.common.authorization;

import com.klabis.common.users.Authority;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Builds snapshots with grants taken as given, for tests outside {@code common.authorization}. Production code
 * cannot do this: its snapshots go through the filtering of {@link AuthorizationSnapshot#fromSources}.
 */
public final class TestSnapshots {

    private TestSnapshots() {
    }

    /**
     * Mirrors the decision of SelfRelationshipSource (slice and cross-module tests have no relationship sources);
     * SelfRelationshipSourceTest guards the two against drifting apart.
     */
    public static Map<Authority, Set<TargetRef>> selfGrants(UUID id, boolean isMember, boolean isMinor) {
        Set<TargetRef> self = Set.of(TargetRef.member(id));
        Map<Authority, Set<TargetRef>> grants = new EnumMap<>(Authority.class);
        grants.put(Authority.MEMBERS_VIEW_PROFILE, self);
        if (isMember) {
            grants.put(Authority.EVENTS_REGISTER_MEMBER, self);
        }
        if (!isMinor) {
            grants.put(Authority.MEMBERS_EDIT_PROFILE, self);
        }
        if (isMember && !isMinor) {
            grants.put(Authority.MEMBERS_SELECT_MEMBERSHIP_FEE_TYPE, self);
        }
        return grants;
    }

    public static AuthorizationSnapshot of(Set<Authority> overAll, Map<Authority, Set<TargetRef>> overTargets) {
        return AuthorizationSnapshot.of(overAll, overTargets);
    }
}
