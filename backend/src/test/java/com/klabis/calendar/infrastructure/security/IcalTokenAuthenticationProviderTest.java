package com.klabis.calendar.infrastructure.security;

import com.klabis.calendar.application.IcalTokenPort;
import com.klabis.common.users.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("IcalTokenAuthenticationProvider")
@ExtendWith(MockitoExtension.class)
class IcalTokenAuthenticationProviderTest {

    @Mock
    private IcalTokenPort icalTokenPort;

    private IcalTokenAuthenticationProvider provider;

    private static final UserId USER_ID = new UserId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        provider = new IcalTokenAuthenticationProvider(icalTokenPort);
    }

    @Test
    @DisplayName("valid token → returns authenticated token with UserId as principal")
    void validToken_returnsAuthenticatedToken() {
        when(icalTokenPort.validate("validraw")).thenReturn(Optional.of(USER_ID));

        Authentication result = provider.authenticate(new IcalTokenAuthenticationToken("validraw"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isEqualTo(USER_ID);
        assertThat(result).isInstanceOf(IcalTokenAuthenticationToken.class);
    }

    @Test
    @DisplayName("invalid token → throws BadCredentialsException")
    void invalidToken_throwsBadCredentials() {
        when(icalTokenPort.validate("wrongtoken")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.authenticate(new IcalTokenAuthenticationToken("wrongtoken")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("supports IcalTokenAuthenticationToken")
    void supportsIcalTokenAuthenticationToken() {
        assertThat(provider.supports(IcalTokenAuthenticationToken.class)).isTrue();
    }

    @Test
    @DisplayName("does not support other token types")
    void doesNotSupportOtherTokenTypes() {
        assertThat(provider.supports(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken.class)).isFalse();
    }
}
