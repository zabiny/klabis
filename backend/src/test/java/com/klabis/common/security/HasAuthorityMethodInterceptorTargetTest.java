package com.klabis.common.security;

import org.springframework.beans.factory.BeanFactory;
import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.authorization.AuthorizationSnapshot;
import com.klabis.common.authorization.AuthorizationSnapshotProvider;
import com.klabis.common.authorization.TargetId;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(HasAuthorityMethodInterceptorTargetTest.Configuration.class)
@DisplayName("HasAuthorityMethodInterceptor over any-of authorities and targeted grants")
class HasAuthorityMethodInterceptorTargetTest {

    private static final UUID MY_ID = UUID.randomUUID();
    private static final UUID CHILD_ID = UUID.randomUUID();
    private static final UUID STRANGER_ID = UUID.randomUUID();

    @Autowired
    private GuardedService service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Set<Authority> overAll, Map<Authority, Set<TargetRef>> overTargets) {
        SecurityContextHolder.getContext().setAuthentication(KlabisAuthenticationFactory.createAuthenticationToken(
                JwtParams.jwtTokenParams("ZBM8001", MY_ID), AuthorizationSnapshot.of(overAll, overTargets)));
    }

    private void authenticateOverAll(Authority... authorities) {
        authenticate(Set.of(authorities), Map.of());
    }

    private void authenticateOverTargets(Authority authority, UUID... memberIds) {
        Set<TargetRef> targets = new java.util.HashSet<>();
        for (UUID id : memberIds) {
            targets.add(TargetRef.member(id));
        }
        authenticate(Set.of(), Map.of(authority, targets));
    }

    @Nested
    @DisplayName("list of authorities")
    class AnyOfAuthorities {

        @Test
        void shouldAllowWhenAnyListedAuthorityIsHeld() {
            authenticateOverAll(Authority.MEMBERS_READ);

            assertThat(service.anyOf()).isEqualTo("ok");
        }

        @Test
        void shouldAllowWhenAnotherListedAuthorityIsHeld() {
            authenticateOverAll(Authority.MEMBERS_MANAGE);

            assertThat(service.anyOf()).isEqualTo("ok");
        }

        @Test
        void shouldDenyWhenNoListedAuthorityIsHeld() {
            authenticateOverAll(Authority.EVENTS_READ);

            assertThatThrownBy(service::anyOf).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void shouldNotAcceptAGrantOverATargetWithoutATargetParameter() {
            authenticateOverTargets(Authority.EVENTS_REGISTRATIONS, CHILD_ID);

            assertThatThrownBy(service::untargeted).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("targeted grants")
    class TargetedGrants {

        @Test
        void shouldAllowGrantOverEverythingForAnyTarget() {
            authenticateOverAll(Authority.EVENTS_REGISTRATIONS);

            assertThat(service.targeted(STRANGER_ID)).isEqualTo("ok");
        }

        @Test
        void shouldAllowGrantOverTheTarget() {
            authenticateOverTargets(Authority.EVENTS_REGISTRATIONS, CHILD_ID);

            assertThat(service.targeted(CHILD_ID)).isEqualTo("ok");
        }

        @Test
        void shouldDenyGrantOverAnotherTarget() {
            authenticateOverTargets(Authority.EVENTS_REGISTRATIONS, CHILD_ID);

            assertThatThrownBy(() -> service.targeted(STRANGER_ID)).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void shouldDenyWhenTargetArgumentIsNull() {
            authenticateOverTargets(Authority.EVENTS_REGISTRATIONS, CHILD_ID);

            assertThatThrownBy(() -> service.targeted(null)).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void shouldAllowGrantOverTheTargetOfAListedAuthority() {
            authenticateOverTargets(Authority.EVENTS_REGISTRATIONS, CHILD_ID);

            assertThat(service.targetedAnyOf(CHILD_ID)).isEqualTo("ok");
            assertThatThrownBy(() -> service.targetedAnyOf(STRANGER_ID)).isInstanceOf(AccessDeniedException.class);
        }

    }

    @Nested
    @DisplayName("owner visible")
    class OwnerVisibleTarget {

        @Test
        void shouldAllowTheUserThemselfWithoutAnyAuthority() {
            authenticate(Set.of(), Map.of());

            assertThat(service.ownerVisible(MY_ID)).isEqualTo("ok");
        }

        @Test
        void shouldDenyAnotherMemberWithoutAuthority() {
            authenticate(Set.of(), Map.of());

            assertThatThrownBy(() -> service.ownerVisible(STRANGER_ID)).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void shouldAllowAnotherMemberWithGrantOverTheTarget() {
            authenticateOverTargets(Authority.EVENTS_REGISTRATIONS, CHILD_ID);

            assertThat(service.ownerVisible(CHILD_ID)).isEqualTo("ok");
        }
    }

    static class GuardedService {

        @HasAuthority({Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
        String anyOf() {
            return "ok";
        }

        @HasAuthority(Authority.EVENTS_REGISTRATIONS)
        String untargeted() {
            return "ok";
        }

        @HasAuthority(Authority.EVENTS_REGISTRATIONS)
        String targeted(@TargetId(TargetType.MEMBER) UUID memberId) {
            return "ok";
        }

        @HasAuthority({Authority.MEMBERS_READ, Authority.EVENTS_REGISTRATIONS})
        String targetedAnyOf(@TargetId(TargetType.MEMBER) UUID memberId) {
            return "ok";
        }

        @OwnerVisible
        @HasAuthority(Authority.EVENTS_REGISTRATIONS)
        String ownerVisible(@TargetId(TargetType.MEMBER) UUID memberId) {
            return "ok";
        }
    }

    @org.springframework.boot.test.context.TestConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    @Import({AuthorizationEvaluator.class, AuthorizationSnapshotProvider.class})
    static class Configuration {

        @Bean
        GuardedService guardedService() {
            return new GuardedService();
        }

        @Bean
        HasAuthorityMethodInterceptor hasAuthorityMethodInterceptor(BeanFactory beanFactory) {
            return new HasAuthorityMethodInterceptor(beanFactory);
        }

        @Bean
        static org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator() {
            var creator = new org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator();
            creator.setProxyTargetClass(true);
            return creator;
        }
    }
}
