package com.klabis.common.authorization;

import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.users.Authority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthorizationEvaluator")
class AuthorizationEvaluatorTest {

    private static final Authority DELEGATED = Authority.EVENTS_REGISTRATIONS;

    private final UUID myId = UUID.randomUUID();
    private final TargetRef me = TargetRef.member(myId);
    private final TargetRef child = TargetRef.member(UUID.randomUUID());
    private final TargetRef stranger = TargetRef.member(UUID.randomUUID());

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AuthorizationEvaluator evaluatorOver(AuthorizationSnapshot snapshot) {
        SecurityContextHolder.getContext().setAuthentication(
                KlabisAuthenticationFactory.createAuthenticationToken(JwtParams.member(myId), snapshot));
        var noBeans = new StaticListableBeanFactory();
        return new AuthorizationEvaluator(new AuthorizationSnapshotProvider(), noBeans.getBeanProvider(ConversionService.class));
    }

    private static AuthorizationSnapshot overAll(Authority... authorities) {
        return AuthorizationSnapshot.of(Set.of(authorities), Map.of());
    }

    private AuthorizationSnapshot delegatedOver(TargetRef... targets) {
        return AuthorizationSnapshot.of(Set.of(), Map.of(DELEGATED, Set.of(targets)));
    }

    @Nested
    @DisplayName("has")
    class Has {

        @Test
        void shouldAnswerGlobalQuestionFromGrantsOverEverythingOnly() {
            assertThat(evaluatorOver(overAll(Authority.MEMBERS_MANAGE)).has(Authority.MEMBERS_MANAGE)).isTrue();
            assertThat(evaluatorOver(overAll(Authority.MEMBERS_MANAGE)).has(Authority.EVENTS_MANAGE)).isFalse();
            assertThat(evaluatorOver(delegatedOver(child)).has(DELEGATED)).isFalse();
        }

        @Test
        void shouldAnswerTargetQuestionFromEverythingOrTargetGrant() {
            assertThat(evaluatorOver(overAll(DELEGATED)).has(DELEGATED, stranger)).isTrue();
            assertThat(evaluatorOver(delegatedOver(child)).has(DELEGATED, child)).isTrue();
            assertThat(evaluatorOver(delegatedOver(child)).has(DELEGATED, stranger)).isFalse();
        }
    }

    @Nested
    @DisplayName("isSelf")
    class IsSelf {

        @Test
        void shouldBeTrueForOwnMemberTarget() {

            assertThat(evaluatorOver(AuthorizationSnapshot.empty()).isSelf(me)).isTrue();
        }

        @Test
        void shouldBeFalseForSomeoneElse() {

            assertThat(evaluatorOver(AuthorizationSnapshot.empty()).isSelf(stranger)).isFalse();
        }

        @Test
        void shouldBeFalseForTargetOfAnotherType() {

            assertThat(evaluatorOver(AuthorizationSnapshot.empty()).isSelf(TargetRef.event(myId))).isFalse();
        }

        @Test
        void shouldBeFalseWithoutAuthentication() {
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());
            SecurityContextHolder.clearContext();

            assertThat(evaluator.isSelf(me)).isFalse();
        }

        @Test
        void shouldBeFalseForTokenWithoutUser() {
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());
            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken("client", "n/a", Authority.MEMBERS_MANAGE.getValue()));

            assertThat(evaluator.isSelf(me)).isFalse();
        }
    }

    @Nested
    @DisplayName("authentication that carries no user snapshot")
    class WithoutAuthentication {

        @Test
        void shouldGrantNothing() {
            var evaluator = evaluatorOver(overAll(Authority.MEMBERS_MANAGE));
            SecurityContextHolder.clearContext();

            assertThat(evaluator.has(Authority.MEMBERS_MANAGE)).isFalse();
            assertThat(evaluator.isAllowed(List.of(Authority.MEMBERS_MANAGE), child, true)).isFalse();
        }

        @Test
        void shouldUseAuthoritiesOfNonUserToken() {
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());
            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken("client", "n/a", Authority.SYNC_MANAGE.getValue(), "FACTOR_PASSWORD"));

            assertThat(evaluator.has(Authority.SYNC_MANAGE)).isTrue();
            assertThat(evaluator.has(Authority.MEMBERS_MANAGE)).isFalse();
        }
    }

    @Nested
    @DisplayName("isAllowed")
    class IsAllowed {

        @Test
        void shouldAllowWhenAnyOfAuthoritiesIsHeldOverEverything() {
            var evaluator = evaluatorOver(overAll(Authority.MEMBERS_MANAGE));

            assertThat(evaluator.isAllowed(List.of(DELEGATED, Authority.MEMBERS_MANAGE), null, false)).isTrue();
            assertThat(evaluator.isAllowed(List.of(DELEGATED, Authority.MEMBERS_MANAGE), child, false)).isTrue();
        }

        @Test
        void shouldDenyWhenNoneOfAuthoritiesIsHeld() {
            var evaluator = evaluatorOver(overAll(Authority.EVENTS_MANAGE));

            assertThat(evaluator.isAllowed(List.of(DELEGATED, Authority.MEMBERS_MANAGE), child, false)).isFalse();
            assertThat(evaluator.isAllowed(List.of(DELEGATED, Authority.MEMBERS_MANAGE), null, false)).isFalse();
        }

        @Test
        void shouldAllowTargetedGrantOnlyForThatTarget() {
            var evaluator = evaluatorOver(delegatedOver(child));

            assertThat(evaluator.isAllowed(List.of(Authority.MEMBERS_MANAGE, DELEGATED), child, false)).isTrue();
            assertThat(evaluator.isAllowed(List.of(Authority.MEMBERS_MANAGE, DELEGATED), stranger, false)).isFalse();
        }

        @Test
        void shouldNotLetTargetedGrantSatisfyOperationWithoutTarget() {
            var evaluator = evaluatorOver(delegatedOver(child));

            assertThat(evaluator.isAllowed(List.of(DELEGATED), null, false)).isFalse();
        }

        @Test
        void shouldAllowSelfWhenOwnerVisible() {
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());

            assertThat(evaluator.isAllowed(List.of(Authority.MEMBERS_MANAGE), me, true)).isTrue();
            assertThat(evaluator.isAllowed(List.of(), me, true)).isTrue();
        }

        @Test
        void shouldDenyOtherThanSelfWhenOwnerVisible() {
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());

            assertThat(evaluator.isAllowed(List.of(Authority.MEMBERS_MANAGE), stranger, true)).isFalse();
        }

        @Test
        void shouldNotTreatOwnerVisibleAsSelfWithoutTarget() {
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());

            assertThat(evaluator.isAllowed(List.of(Authority.MEMBERS_MANAGE), null, true)).isFalse();
        }

        @Test
        void shouldNotAllowSelfWhenNotOwnerVisible() {
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());

            assertThat(evaluator.isAllowed(List.of(Authority.MEMBERS_MANAGE), me, false)).isFalse();
        }

        @Test
        void shouldDenyWhenNothingGrantsAccess() {
            assertThat(evaluatorOver(overAll(Authority.MEMBERS_MANAGE)).isAllowed(List.of(), child, false)).isFalse();
        }
    }
}
