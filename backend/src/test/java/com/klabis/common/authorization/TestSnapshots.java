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
     * What SelfRelationshipSource grants an adult member over themself; slice and cross-module tests have no
     * relationship sources.
     */
    public static Map<Authority, Set<TargetRef>> selfGrantsOf(UUID memberId) {
        Map<Authority, Set<TargetRef>> grants = new EnumMap<>(minorSelfGrantsOf(memberId));
        grants.putAll(nonMemberSelfGrantsOf(memberId));
        grants.put(Authority.MEMBERS_SELECT_MEMBERSHIP_FEE_TYPE, Set.of(TargetRef.member(memberId)));
        return grants;
    }

    /** A minor member may view their profile and register for events, but neither edit it nor choose a fee type. */
    public static Map<Authority, Set<TargetRef>> minorSelfGrantsOf(UUID memberId) {
        Map<Authority, Set<TargetRef>> grants = new EnumMap<>(Authority.class);
        grants.put(Authority.MEMBERS_VIEW_PROFILE, Set.of(TargetRef.member(memberId)));
        grants.put(Authority.EVENTS_REGISTER_MEMBER, Set.of(TargetRef.member(memberId)));
        return grants;
    }

    /** A user without a member profile (a legal guardian) holds only the profile permissions over themself. */
    public static Map<Authority, Set<TargetRef>> nonMemberSelfGrantsOf(UUID userId) {
        Set<TargetRef> self = Set.of(TargetRef.member(userId));
        return Map.of(
                Authority.MEMBERS_VIEW_PROFILE, self,
                Authority.MEMBERS_EDIT_PROFILE, self);
    }

    public static AuthorizationSnapshot of(Set<Authority> overAll, Map<Authority, Set<TargetRef>> overTargets) {
        return AuthorizationSnapshot.of(overAll, overTargets);
    }
}
