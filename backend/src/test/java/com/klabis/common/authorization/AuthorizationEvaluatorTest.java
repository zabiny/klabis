package com.klabis.common.authorization;

import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.security.fieldsecurity.OwnershipResolver;
import com.klabis.common.users.Authority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
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

    private void authenticateAsMember() {
        SecurityContextHolder.getContext().setAuthentication(KlabisAuthenticationFactory.createAuthenticationToken(
                JwtParams.member(myId)));
    }

    private AuthorizationEvaluator evaluatorOver(AuthorizationSnapshot snapshot) {
        OwnershipResolver resolver = (ownerId, authentication) ->
                authentication instanceof com.klabis.common.security.KlabisJwtAuthenticationToken token
                && token.getMemberIdUuid().filter(ownerId::equals).isPresent();
        var beans = new StaticListableBeanFactory(Map.of("resolver", resolver));
        return new AuthorizationEvaluator(() -> snapshot, beans.getBeanProvider(OwnershipResolver.class));
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
            authenticateAsMember();

            assertThat(evaluatorOver(AuthorizationSnapshot.empty()).isSelf(me)).isTrue();
        }

        @Test
        void shouldBeFalseForSomeoneElse() {
            authenticateAsMember();

            assertThat(evaluatorOver(AuthorizationSnapshot.empty()).isSelf(stranger)).isFalse();
        }

        @Test
        void shouldBeFalseForTargetOfAnotherType() {
            authenticateAsMember();

            assertThat(evaluatorOver(AuthorizationSnapshot.empty()).isSelf(TargetRef.event(myId))).isFalse();
        }

        @Test
        void shouldBeFalseWithoutAuthentication() {
            assertThat(evaluatorOver(AuthorizationSnapshot.empty()).isSelf(me)).isFalse();
        }

        @Test
        void shouldBeFalseWhenNoOwnershipResolverIsAvailable() {
            authenticateAsMember();
            var evaluator = new AuthorizationEvaluator(AuthorizationSnapshot::empty,
                    new StaticListableBeanFactory().getBeanProvider(OwnershipResolver.class));

            assertThat(evaluator.isSelf(me)).isFalse();
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
            authenticateAsMember();
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());

            assertThat(evaluator.isAllowed(List.of(Authority.MEMBERS_MANAGE), me, true)).isTrue();
            assertThat(evaluator.isAllowed(List.of(), me, true)).isTrue();
        }

        @Test
        void shouldDenyOtherThanSelfWhenOwnerVisible() {
            authenticateAsMember();
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());

            assertThat(evaluator.isAllowed(List.of(Authority.MEMBERS_MANAGE), stranger, true)).isFalse();
        }

        @Test
        void shouldNotTreatOwnerVisibleAsSelfWithoutTarget() {
            authenticateAsMember();
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());

            assertThat(evaluator.isAllowed(List.of(Authority.MEMBERS_MANAGE), null, true)).isFalse();
        }

        @Test
        void shouldNotAllowSelfWhenNotOwnerVisible() {
            authenticateAsMember();
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());

            assertThat(evaluator.isAllowed(List.of(Authority.MEMBERS_MANAGE), me, false)).isFalse();
        }

        @Test
        void shouldDenyWhenNothingGrantsAccess() {
            assertThat(evaluatorOver(overAll(Authority.MEMBERS_MANAGE)).isAllowed(List.of(), child, false)).isFalse();
        }
    }
}
