package com.klabis.common.security;

import com.klabis.common.authorization.AuthorizationSnapshot;
import com.klabis.common.authorization.AuthorizationSnapshotProvider;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.common.users.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(SnapshotAuthoritiesInMethodSecurityTest.Configuration.class)
@DisplayName("Spring method security over authorities delegated to the request snapshot")
class SnapshotAuthoritiesInMethodSecurityTest {

    private static final AtomicReference<AuthorizationSnapshot> SNAPSHOT = new AtomicReference<>();

    @Autowired
    private GuardedService guardedService;

    @Autowired
    private KlabisJwtAuthenticationConverter converter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateWithSnapshot(Authority... overAll) {
        SNAPSHOT.set(AuthorizationSnapshot.of(Set.of(overAll), Map.of()));
        Jwt jwt = Jwt.withTokenValue("t").header("alg", JwsAlgorithms.RS256)
                .subject("ZBM8001").claim("user_id", UserId.newId().uuid().toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(converter.convert(jwt));
    }

    @Test
    @DisplayName("@PreAuthorize hasAuthority passes for an authority held in the snapshot")
    void preAuthorizeAllowsSnapshotAuthority() {
        authenticateWithSnapshot(Authority.MEMBERS_READ);

        assertThat(guardedService.preAuthorized()).isEqualTo("ok");
    }

    @Test
    @DisplayName("@PreAuthorize hasAuthority refuses an authority the snapshot does not hold")
    void preAuthorizeRefusesMissingAuthority() {
        authenticateWithSnapshot(Authority.EVENTS_READ);

        assertThatThrownBy(guardedService::preAuthorized).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("@PreAuthorize follows a change of the snapshot between calls")
    void preAuthorizeFollowsSnapshotChange() {
        authenticateWithSnapshot(Authority.MEMBERS_READ);
        assertThat(guardedService.preAuthorized()).isEqualTo("ok");

        SNAPSHOT.set(AuthorizationSnapshot.empty());

        assertThatThrownBy(guardedService::preAuthorized).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("@HasAuthority sees the same authorities as @PreAuthorize")
    void hasAuthorityAgreesWithPreAuthorize() {
        authenticateWithSnapshot(Authority.MEMBERS_READ);
        assertThat(guardedService.hasAuthorityGuarded()).isEqualTo("ok");

        authenticateWithSnapshot(Authority.EVENTS_READ);
        assertThatThrownBy(guardedService::hasAuthorityGuarded).isInstanceOf(AccessDeniedException.class);
    }

    static class GuardedService {

        @PreAuthorize("hasAuthority('MEMBERS:READ')")
        String preAuthorized() {
            return "ok";
        }

        @HasAuthority(Authority.MEMBERS_READ)
        String hasAuthorityGuarded() {
            return "ok";
        }
    }

    @org.springframework.boot.test.context.TestConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    @org.springframework.context.annotation.Import(com.klabis.common.authorization.AuthorizationEvaluator.class)
    static class Configuration {

        @Bean
        GuardedService guardedService() {
            return new GuardedService();
        }

        @Bean
        HasAuthorityMethodInterceptor hasAuthorityMethodInterceptor() {
            return new HasAuthorityMethodInterceptor();
        }

        @Bean
        static org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator() {
            var creator = new org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator();
            creator.setProxyTargetClass(true);
            return creator;
        }

        @Bean
        AuthorizationSnapshotProvider snapshotProvider() {
            return SNAPSHOT::get;
        }

        @Bean
        KlabisJwtAuthenticationConverter converter(
                org.springframework.beans.factory.ObjectProvider<AuthorizationSnapshotProvider> provider) {
            return new KlabisJwtAuthenticationConverter(provider);
        }
    }
}
