package com.klabis.common.authorization;

import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.security.fieldsecurity.ReadAuthority;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
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

    record Payload(String open,
                   @HasAuthority({Authority.MEMBERS_MANAGE, Authority.EVENTS_REGISTRATIONS}) @OwnerVisible String secured,
                   @HasAuthority(Authority.MEMBERS_MANAGE) @ReadAuthority(Authority.MEMBERS_READ) String readable) {
    }

    static class Operations {

        @HasAuthority({Authority.MEMBERS_MANAGE, Authority.EVENTS_REGISTRATIONS})
        @OwnerVisible
        public void targeted(@TargetId(TargetType.MEMBER) UUID id) {
        }

        @HasAuthority(Authority.EVENTS_MANAGE)
        public void global() {
        }

        public void open() {
        }
    }

    @Nested
    @DisplayName("requestFieldAccess")
    class RequestFieldAccess {

        @Test
        void shouldBeWriteForUnsecuredProperty() {
            assertThat(evaluatorOver(AuthorizationSnapshot.empty()).requestFieldAccess(Payload.class, "open", null))
                    .isEqualTo(FieldAccess.WRITE);
            assertThat(evaluatorOver(AuthorizationSnapshot.empty()).requestFieldAccess(Payload.class, "missing", null))
                    .isEqualTo(FieldAccess.WRITE);
        }

        @Test
        void shouldBeWriteWithAuthorityOverTarget() {
            assertThat(evaluatorOver(delegatedOver(child)).requestFieldAccess(Payload.class, "secured", child))
                    .isEqualTo(FieldAccess.WRITE);
            assertThat(evaluatorOver(delegatedOver(child)).requestFieldAccess(Payload.class, "secured", stranger))
                    .isEqualTo(FieldAccess.NONE);
        }

        @Test
        void shouldBeReadWithReadAuthorityOnly() {
            assertThat(evaluatorOver(overAll(Authority.MEMBERS_READ)).requestFieldAccess(Payload.class, "readable", null))
                    .isEqualTo(FieldAccess.READ);
            assertThat(evaluatorOver(overAll(Authority.MEMBERS_MANAGE)).requestFieldAccess(Payload.class, "readable", null))
                    .isEqualTo(FieldAccess.WRITE);
            assertThat(evaluatorOver(AuthorizationSnapshot.empty()).requestFieldAccess(Payload.class, "readable", null))
                    .isEqualTo(FieldAccess.NONE);
        }
    }

    @Nested
    @DisplayName("canReadField by property")
    class CanReadFieldByProperty {

        @Test
        void shouldBeAllowedForUnsecuredOrUnknownProperty() {
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());

            assertThat(evaluator.canReadField(Payload.class, "open", child)).isTrue();
            assertThat(evaluator.canReadField(Payload.class, "missing", child)).isTrue();
        }

        @Test
        void shouldFollowTheSameRuleAsTheSerializedField() {
            assertThat(evaluatorOver(delegatedOver(child)).canReadField(Payload.class, "secured", child)).isTrue();
            assertThat(evaluatorOver(delegatedOver(child)).canReadField(Payload.class, "secured", stranger)).isFalse();
            assertThat(evaluatorOver(overAll(Authority.MEMBERS_MANAGE)).canReadField(Payload.class, "secured", stranger)).isTrue();
        }

        @Test
        void shouldBeAllowedToOwnerOfOwnerVisibleField() {
            assertThat(evaluatorOver(AuthorizationSnapshot.empty()).canReadField(Payload.class, "secured", me)).isTrue();
            assertThat(evaluatorOver(AuthorizationSnapshot.empty()).canReadField(Payload.class, "secured", stranger)).isFalse();
        }

        @Test
        void shouldIgnoreReadAuthorityBecauseItOnlyAffectsRequestTemplates() {
            assertThat(evaluatorOver(overAll(Authority.MEMBERS_READ)).canReadField(Payload.class, "readable", child)).isFalse();
        }
    }

    @Nested
    @DisplayName("describeRequirement")
    class DescribeRequirement {

        @Test
        void shouldDescribeOperationWithAuthoritiesTargetAndOwner() throws NoSuchMethodException {
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());

            assertThat(evaluator.describeRequirement(Operations.class.getMethod("targeted", UUID.class), Operations.class))
                    .isEqualTo("authority MEMBERS:MANAGE or EVENTS:REGISTRATIONS (over everything or over the MEMBER), or being the owner");
        }

        @Test
        void shouldDescribeOperationWithAuthorityOnly() throws NoSuchMethodException {
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());

            assertThat(evaluator.describeRequirement(Operations.class.getMethod("global"), Operations.class))
                    .isEqualTo("authority EVENTS:MANAGE");
        }

        @Test
        void shouldDescribeField() throws NoSuchMethodException {
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());

            assertThat(evaluator.describeRequirement(Payload.class.getMethod("secured")))
                    .isEqualTo("authority MEMBERS:MANAGE or EVENTS:REGISTRATIONS, or being the owner");
        }
    }

    @Nested
    @DisplayName("guarded and targets")
    class GuardedAndTargets {

        @Test
        void shouldTellGuardedFromOpenMethods() throws NoSuchMethodException {
            assertThat(AuthorizationEvaluator.isGuarded(Operations.class.getMethod("global"), Operations.class)).isTrue();
            assertThat(AuthorizationEvaluator.isGuarded(Operations.class.getMethod("open"), Operations.class)).isFalse();
        }

        @Test
        void shouldConvertTargetIdThroughTheSameConversionAsArguments() {
            var evaluator = evaluatorOver(AuthorizationSnapshot.empty());

            assertThat(evaluator.toTarget(TargetType.MEMBER, myId)).isEqualTo(me);
            assertThat(evaluator.toTarget(TargetType.MEMBER, null)).isNull();
            assertThat(evaluator.toTarget(TargetType.MEMBER, "not-convertible-without-conversion-service")).isNull();
        }

        @Test
        void shouldConvertStringTargetIdWithConversionService() {
            SecurityContextHolder.getContext().setAuthentication(
                    KlabisAuthenticationFactory.createAuthenticationToken(JwtParams.member(myId), AuthorizationSnapshot.empty()));
            var beans = new StaticListableBeanFactory();
            beans.addBean("conversionService", new DefaultConversionService());
            var evaluator = new AuthorizationEvaluator(new AuthorizationSnapshotProvider(), beans.getBeanProvider(ConversionService.class));

            assertThat(evaluator.toTarget(TargetType.MEMBER, myId.toString())).isEqualTo(me);
            assertThat(evaluator.toTarget(TargetType.MEMBER, "not-a-uuid")).isNull();
        }
    }
}
