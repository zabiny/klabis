package com.klabis.authorizationserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.shaded.org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for OpenID Connect ID token generation configuration.
 *
 * <p><b>Purpose:</b> Verifies that the application is configured to support OpenID Connect
 * ID token generation when the `openid` scope is requested.
 *
 * <p><b>What It Tests:</b>
 * <ul>
 *   <li>AuthorizationServerConfiguration can be instantiated</li>
 *   <li>JWT customizer bean can be created (it will add OIDC claims to ID tokens)</li>
 *   <li>AuthorizationServerSettings bean can be created for OIDC support</li>
 * </ul>
 *
 * <p><b>Full E2E Testing:</b> End-to-end testing of the OIDC flow with bootstrap data
 * and client registration is tested in integration tests.
 */
@DisplayName("OIDC: ID Token Generation Configuration")
class OidcIdTokenGenerationTest {

    @Test
    @DisplayName("should create authorization server settings for OIDC")
    void shouldCreateAuthorizationServerSettings() {
        // Given: AuthorizationServerConfiguration has been instantiated with issuer
        AuthorizationServerConfiguration config = new AuthorizationServerConfiguration(SimpleObjectProvider.empty());
        ReflectionTestUtils.setField(config, "issuer", "https://localhost:8443");

        // Then: authorizationServerSettings bean can be created
        var settings = config.authorizationServerSettings();
        assertThat(settings).isNotNull();
        assertThat(settings.getIssuer()).isEqualTo("https://localhost:8443");
    }

    @Test
    @DisplayName("should provide JWT customizer bean")
    void shouldProvideJwtCustomizer() {
        // Given: AuthorizationServerConfiguration has been instantiated
        AuthorizationServerConfiguration config = new AuthorizationServerConfiguration(SimpleObjectProvider.empty());

        // Then: jwtCustomizer bean can be created (it will add ID token claims)
        var customizer = config.jwtCustomizer();
        assertThat(customizer).isNotNull();
    }
}

class SimpleObjectProvider<T> implements ObjectProvider<T> {
    public static <T> ObjectProvider<T> empty() {
        return new SimpleObjectProvider<>(null);
    }

    public static <T> ObjectProvider<T> of(T t) {
        return new SimpleObjectProvider<>(t);
    };

    private final T object;

    private SimpleObjectProvider(@Nullable T object) {
        this.object = object;
    }


    @Override
    public Stream<T> stream() {
        return Optional.ofNullable(object).stream();
    }
}
