package com.klabis.common.bootstrap;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest(verifyAutomatically = false)
@Import(TestApplicationConfiguration.class)
@ActiveProfiles({"test", "local-dev"})
@TestPropertySource(properties = {
        "klabis.bootstrap.admin.username=admin",
        "klabis.bootstrap.admin.password=admin123",
        "klabis.oauth2.client.secret=test-secret-123"
})
@DisplayName("Local-dev confidential client bootstrap")
@CleanupTestData
class LocalDevRefreshTokenFlowTest {

    private static final String LOCAL_CLIENT_ID = "klabis-web-local";

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private BootstrapDataLoader bootstrapDataLoader;

    @BeforeEach
    void ensureBootstrapData() {
        bootstrapDataLoader.run(new DefaultApplicationArguments());
    }

    @Test
    @DisplayName("should register klabis-web-local when local-dev profile is active")
    void shouldRegisterLocalDevClient() {
        RegisteredClient client = registeredClientRepository.findByClientId(LOCAL_CLIENT_ID);

        assertThat(client)
                .as("OAuth2 client '%s' should be registered when local-dev profile is active", LOCAL_CLIENT_ID)
                .isNotNull();
    }

    @Test
    @DisplayName("should register klabis-web-local with REFRESH_TOKEN grant type")
    void shouldRegisterLocalDevClientWithRefreshTokenGrant() {
        RegisteredClient client = registeredClientRepository.findByClientId(LOCAL_CLIENT_ID);

        assertThat(client).isNotNull();
        assertThat(client.getAuthorizationGrantTypes())
                .as("local-dev client must support REFRESH_TOKEN to enable silent token renewal")
                .contains(AuthorizationGrantType.REFRESH_TOKEN);
    }

    @Test
    @DisplayName("should register klabis-web-local with AUTHORIZATION_CODE grant type")
    void shouldRegisterLocalDevClientWithAuthorizationCodeGrant() {
        RegisteredClient client = registeredClientRepository.findByClientId(LOCAL_CLIENT_ID);

        assertThat(client).isNotNull();
        assertThat(client.getAuthorizationGrantTypes())
                .contains(AuthorizationGrantType.AUTHORIZATION_CODE);
    }

    @Test
    @DisplayName("should register klabis-web-local without CLIENT_CREDENTIALS grant type")
    void shouldNotHaveClientCredentialsGrant() {
        RegisteredClient client = registeredClientRepository.findByClientId(LOCAL_CLIENT_ID);

        assertThat(client).isNotNull();
        assertThat(client.getAuthorizationGrantTypes())
                .as("local-dev client must not have CLIENT_CREDENTIALS grant")
                .doesNotContain(AuthorizationGrantType.CLIENT_CREDENTIALS);
    }

    @Test
    @DisplayName("should register klabis-web-local with CLIENT_SECRET_POST authentication method")
    void shouldRegisterLocalDevClientWithClientSecretPost() {
        RegisteredClient client = registeredClientRepository.findByClientId(LOCAL_CLIENT_ID);

        assertThat(client).isNotNull();
        assertThat(client.getClientAuthenticationMethods())
                .as("local-dev client must use CLIENT_SECRET_POST for token endpoint authentication")
                .contains(ClientAuthenticationMethod.CLIENT_SECRET_POST);
    }

    @Test
    @DisplayName("should register klabis-web-local with PKCE required")
    void shouldRegisterLocalDevClientWithPkce() {
        RegisteredClient client = registeredClientRepository.findByClientId(LOCAL_CLIENT_ID);

        assertThat(client).isNotNull();
        assertThat(client.getClientSettings().isRequireProofKey())
                .as("local-dev client must require PKCE")
                .isTrue();
    }

    @Test
    @DisplayName("should register klabis-web-local with localhost:3000 redirect URI")
    void shouldRegisterLocalDevClientWithLocalhostRedirectUri() {
        RegisteredClient client = registeredClientRepository.findByClientId(LOCAL_CLIENT_ID);

        assertThat(client).isNotNull();
        assertThat(client.getRedirectUris())
                .as("local-dev client must allow localhost:3000 callback for Vite dev server")
                .contains("http://localhost:3000/auth/callback");
    }

    @Test
    @DisplayName("should register klabis-web-local with localhost:3000 post-logout redirect URI")
    void shouldRegisterLocalDevClientWithPostLogoutRedirectUri() {
        RegisteredClient client = registeredClientRepository.findByClientId(LOCAL_CLIENT_ID);

        assertThat(client).isNotNull();
        assertThat(client.getPostLogoutRedirectUris())
                .as("local-dev client must allow localhost:3000 as post-logout redirect for logout to work")
                .contains("http://localhost:3000");
    }

    @Test
    @DisplayName("should be idempotent — second bootstrap does not fail")
    void shouldBeIdempotentOnSecondBootstrap() {
        bootstrapDataLoader.run(new DefaultApplicationArguments());

        RegisteredClient client = registeredClientRepository.findByClientId(LOCAL_CLIENT_ID);
        assertThat(client).isNotNull();
    }
}
