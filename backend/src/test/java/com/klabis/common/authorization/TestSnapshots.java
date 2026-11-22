package com.klabis.common.authorization;

import com.klabis.common.users.Authority;

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
     * What SelfRelationshipSource grants a member over themself that tests exercise; slice and cross-module
     * tests have no relationship sources.
     */
    public static Map<Authority, Set<TargetRef>> selfGrantsOf(UUID memberId) {
        return Map.of(Authority.EVENTS_REGISTER_MEMBER, Set.of(TargetRef.member(memberId)));
    }

    public static AuthorizationSnapshot of(Set<Authority> overAll, Map<Authority, Set<TargetRef>> overTargets) {
        return AuthorizationSnapshot.of(overAll, overTargets);
    }
}
