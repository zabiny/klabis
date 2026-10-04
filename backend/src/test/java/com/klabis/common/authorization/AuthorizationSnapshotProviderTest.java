package com.klabis.common.authorization;

import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.users.Authority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthorizationSnapshotProvider")
class AuthorizationSnapshotProviderTest {

    private final AuthorizationSnapshotProvider provider = new AuthorizationSnapshotProvider();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("returns the snapshot carried by the user token, the same instance for every call")
    void shouldReturnSnapshotOfUserToken() {
        var snapshot = AuthorizationSnapshot.of(Set.of(Authority.MEMBERS_MANAGE), Map.of());
        SecurityContextHolder.getContext().setAuthentication(KlabisAuthenticationFactory.createAuthenticationToken(
                JwtParams.jwtTokenParams("ZBM8001", UUID.randomUUID()), snapshot));

        assertThat(provider.current()).isSameAs(snapshot);
        assertThat(provider.current()).isSameAs(snapshot);
    }

    @Test
    @DisplayName("client_credentials style authentication uses its known authorities and no targeted grants")
    void shouldUseAuthoritiesOfNonUserAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("client", "n/a", Authority.SYNC_MANAGE.getValue(), "FACTOR_PASSWORD"));

        assertThat(provider.current().overAll()).containsExactly(Authority.SYNC_MANAGE);
    }

    @Test
    @DisplayName("grants nothing when there is no authentication")
    void shouldGrantNothingWithoutAuthentication() {
        assertThat(provider.current().overAll()).isEmpty();
    }

    @Test
    @DisplayName("grants nothing for an unauthenticated token")
    void shouldGrantNothingForUnauthenticatedToken() {
        var token = new TestingAuthenticationToken("u", "p", Authority.MEMBERS_MANAGE.getValue());
        token.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThat(provider.current().overAll()).isEmpty();
    }
}
