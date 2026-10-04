package com.klabis.common.security;

import org.springframework.beans.factory.BeanFactory;
import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.authorization.TestSnapshots;
import com.klabis.common.authorization.AuthorizationSnapshotProvider;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(SnapshotAuthoritiesInMethodSecurityTest.Configuration.class)
@DisplayName("Spring method security over authorities delegated to the request snapshot")
class SnapshotAuthoritiesInMethodSecurityTest {

    @Autowired
    private GuardedService guardedService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateWithSnapshot(Authority... overAll) {
        SecurityContextHolder.getContext().setAuthentication(KlabisAuthenticationFactory.createAuthenticationToken(
                JwtParams.jwtTokenParams("ZBM8001", UUID.randomUUID()),
                TestSnapshots.of(Set.of(overAll), Map.of())));
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
    @DisplayName("@PreAuthorize follows the snapshot of the token of the next request")
    void preAuthorizeFollowsSnapshotOfNextToken() {
        authenticateWithSnapshot(Authority.MEMBERS_READ);
        assertThat(guardedService.preAuthorized()).isEqualTo("ok");

        authenticateWithSnapshot();

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
    @org.springframework.context.annotation.Import({AuthorizationEvaluator.class, AuthorizationSnapshotProvider.class})
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
