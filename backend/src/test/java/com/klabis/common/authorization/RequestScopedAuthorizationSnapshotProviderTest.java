package com.klabis.common.authorization;

import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.application.PermissionService;
import com.klabis.common.users.domain.UserPermissions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("RequestScopedAuthorizationSnapshotProvider")
class RequestScopedAuthorizationSnapshotProviderTest {

    private final PermissionService permissionService = mock(PermissionService.class);
    private final RelationshipSource source = mock(RelationshipSource.class);
    private final UserId userId = UserId.newId();

    private RequestScopedAuthorizationSnapshotProvider provider() {
        var beans = new StaticListableBeanFactory(Map.of("source", source));
        return new RequestScopedAuthorizationSnapshotProvider(permissionService,
                beans.getBeanProvider(RelationshipSource.class));
    }

    @BeforeEach
    void setUp() {
        when(permissionService.getUserPermissions(userId))
                .thenReturn(UserPermissions.create(userId, Set.of(Authority.MEMBERS_MANAGE)));
        when(source.grantsOf(userId)).thenReturn(Map.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private void authenticateAsUser() {
        SecurityContextHolder.getContext().setAuthentication(KlabisAuthenticationFactory.createAuthenticationToken(
                JwtParams.jwtTokenParams("ZBM8001", userId).withAuthorities(Authority.EVENTS_MANAGE)));
    }

    @Test
    @DisplayName("loads nothing until the first authorization question")
    void shouldNotLoadWhenNobodyAsks() {
        authenticateAsUser();

        provider();

        verifyNoInteractions(permissionService, source);
    }

    @Test
    @DisplayName("loads permissions and relationship grants of the user once and reuses the snapshot")
    void shouldLoadOnce() {
        authenticateAsUser();
        var provider = provider();

        var first = provider.current();
        var second = provider.current();

        assertThat(second).isSameAs(first);
        verify(permissionService, org.mockito.Mockito.times(1)).getUserPermissions(userId);
        verify(source, org.mockito.Mockito.times(1)).grantsOf(userId);
    }

    @Test
    @DisplayName("takes global authorities from stored permissions, not from the token, and adds standard authorities")
    void shouldTakeGlobalAuthoritiesFromStoredPermissions() {
        authenticateAsUser();

        var snapshot = provider().current();

        assertThat(snapshot.overAll()).contains(Authority.MEMBERS_MANAGE)
                .containsAll(Authority.getStandardUserAuthorities())
                .doesNotContain(Authority.EVENTS_MANAGE);
    }

    @Test
    @DisplayName("client_credentials token uses its own authorities, no stored permissions and no targeted grants")
    void shouldUseTokenAuthoritiesForClientCredentials() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("client")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority(Authority.SYNC_MANAGE.getValue()),
                        new SimpleGrantedAuthority("FACTOR_PASSWORD"))));

        var snapshot = provider().current();

        assertThat(snapshot.overAll()).containsExactly(Authority.SYNC_MANAGE);
        verify(permissionService, never()).getUserPermissions(any());
        verifyNoInteractions(source);
    }

    @Test
    @DisplayName("grants nothing when there is no authentication")
    void shouldGrantNothingWithoutAuthentication() {
        var snapshot = provider().current();

        assertThat(snapshot.overAll()).isEmpty();
        verifyNoInteractions(permissionService, source);
    }

    @Test
    @DisplayName("grants nothing for an unauthenticated token")
    void shouldGrantNothingForUnauthenticatedToken() {
        var token = new TestingAuthenticationToken("u", "p", Authority.MEMBERS_MANAGE.getValue());
        token.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThat(provider().current().overAll()).isEmpty();
    }

    @Test
    @DisplayName("is bound to the request scope: one load per request, a fresh one for the next request")
    void shouldLoadOncePerRequest() {
        authenticateAsUser();
        try (var context = new GenericWebApplicationContext(new MockServletContext())) {
            context.registerBean(PermissionService.class, () -> permissionService);
            context.registerBean(RelationshipSource.class, () -> source);
            new AnnotatedBeanDefinitionReader(context).registerBean(RequestScopedAuthorizationSnapshotProvider.class);
            context.refresh();
            var scoped = context.getBean(AuthorizationSnapshotProvider.class);

            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
            var firstRequestSnapshot = scoped.current();
            assertThat(scoped.current()).isSameAs(firstRequestSnapshot);

            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
            scoped.current();

            verify(permissionService, org.mockito.Mockito.times(2)).getUserPermissions(userId);
        }
    }
}
