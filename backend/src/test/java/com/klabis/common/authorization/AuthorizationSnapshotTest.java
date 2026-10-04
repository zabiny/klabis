package com.klabis.common.authorization;

import com.klabis.common.users.Authority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthorizationSnapshot")
class AuthorizationSnapshotTest {

    private static final Authority DELEGATABLE = Authority.EVENTS_REGISTRATIONS;

    private final TargetRef child = TargetRef.member(UUID.randomUUID());
    private final TargetRef otherMember = TargetRef.member(UUID.randomUUID());

    @SafeVarargs
    private static AuthorizationSnapshot fromSources(Set<Authority> overAll, Map<Authority, Set<TargetRef>>... sources) {
        return AuthorizationSnapshot.fromSources(overAll, List.of(sources), delegatable -> delegatable == DELEGATABLE);
    }

    @Nested
    @DisplayName("grant over everything")
    class OverEverything {

        @Test
        @DisplayName("covers every target")
        void shouldCoverEveryTarget() {
            var snapshot = AuthorizationSnapshot.of(Set.of(Authority.MEMBERS_MANAGE), Map.of());

            assertThat(snapshot.hasOverAll(Authority.MEMBERS_MANAGE)).isTrue();
            assertThat(snapshot.has(Authority.MEMBERS_MANAGE, child)).isTrue();
            assertThat(snapshot.has(Authority.MEMBERS_MANAGE, otherMember)).isTrue();
        }

        @Test
        @DisplayName("covers a target created after the snapshot was built")
        void shouldCoverTargetCreatedLater() {
            var snapshot = AuthorizationSnapshot.of(Set.of(Authority.MEMBERS_MANAGE), Map.of());

            assertThat(snapshot.has(Authority.MEMBERS_MANAGE, TargetRef.member(UUID.randomUUID()))).isTrue();
        }

        @Test
        @DisplayName("does not cover other authorities")
        void shouldNotCoverOtherAuthorities() {
            var snapshot = AuthorizationSnapshot.of(Set.of(Authority.MEMBERS_MANAGE), Map.of());

            assertThat(snapshot.hasOverAll(Authority.EVENTS_MANAGE)).isFalse();
            assertThat(snapshot.has(Authority.EVENTS_MANAGE, child)).isFalse();
        }
    }

    @Nested
    @DisplayName("grant over a target")
    class OverTarget {

        @Test
        @DisplayName("covers only that target and is not a grant over everything")
        void shouldCoverOnlyGrantedTarget() {
            var snapshot = fromSources(Set.of(), Map.of(DELEGATABLE, Set.of(child)));

            assertThat(snapshot.has(DELEGATABLE, child)).isTrue();
            assertThat(snapshot.has(DELEGATABLE, otherMember)).isFalse();
            assertThat(snapshot.hasOverAll(DELEGATABLE)).isFalse();
            assertThat(snapshot.targetsOf(DELEGATABLE)).containsExactly(child);
        }

        @Test
        @DisplayName("unites grants of several sources")
        void shouldUniteGrantsOfSeveralSources() {
            var snapshot = fromSources(Set.of(),
                    Map.of(DELEGATABLE, Set.of(child)),
                    Map.of(DELEGATABLE, Set.of(otherMember)));

            assertThat(snapshot.targetsOf(DELEGATABLE)).containsExactlyInAnyOrder(child, otherMember);
            assertThat(snapshot.has(DELEGATABLE, child)).isTrue();
            assertThat(snapshot.has(DELEGATABLE, otherMember)).isTrue();
        }

        @Test
        @DisplayName("ignores a source grant for an authority that can only be held over everything")
        void shouldIgnoreGrantForAllOnlyAuthority() {
            var snapshot = AuthorizationSnapshot.fromSources(Set.of(),
                    List.of(Map.of(Authority.MEMBERS_MANAGE, Set.of(child))));

            assertThat(snapshot.has(Authority.MEMBERS_MANAGE, child)).isFalse();
            assertThat(snapshot.targetsOf(Authority.MEMBERS_MANAGE)).isEmpty();
        }

        @Test
        @DisplayName("ignores a source grant whose target type does not match the authority")
        void shouldIgnoreGrantForWrongTargetType() {
            var event = TargetRef.event(UUID.randomUUID());

            var snapshot = fromSources(Set.of(), Map.of(DELEGATABLE, Set.of(event)));

            assertThat(snapshot.targetsOf(DELEGATABLE)).isEmpty();
        }

        @Test
        @DisplayName("keeps a grant over everything held next to targeted grants of the same authority")
        void shouldCombineEverythingAndTargetedGrants() {
            var snapshot = fromSources(Set.of(DELEGATABLE), Map.of(DELEGATABLE, Set.of(child)));

            assertThat(snapshot.hasOverAll(DELEGATABLE)).isTrue();
            assertThat(snapshot.has(DELEGATABLE, otherMember)).isTrue();
        }
    }

    @Test
    @DisplayName("is immutable")
    void shouldBeImmutable() {
        var snapshot = AuthorizationSnapshot.of(Set.of(Authority.MEMBERS_MANAGE), Map.of(DELEGATABLE, Set.of(child)));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> snapshot.overAll().add(Authority.EVENTS_MANAGE))
                .isInstanceOf(UnsupportedOperationException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> snapshot.targetsOf(DELEGATABLE).add(otherMember))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("empty snapshot grants nothing")
    void shouldGrantNothingWhenEmpty() {
        assertThat(AuthorizationSnapshot.empty().hasOverAll(Authority.MEMBERS_READ)).isFalse();
        assertThat(AuthorizationSnapshot.empty().has(Authority.MEMBERS_READ, child)).isFalse();
    }
}
