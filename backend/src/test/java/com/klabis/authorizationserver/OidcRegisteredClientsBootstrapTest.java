package com.klabis.authorizationserver;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.klabis.common.bootstrap.BootstrapDataLoader;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestApplicationConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "klabis.bootstrap.admin.username=admin",
        "klabis.bootstrap.admin.password=admin123",
        "klabis.oauth2.client.secret=test-secret-123"
})
@DisplayName("OIDC Registered Clients Bootstrap")
@CleanupTestData
class OidcRegisteredClientsBootstrapTest {

    private static final String CLIENT_ID = "klabis-web";

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private BootstrapDataLoader bootstrapDataLoader;

    @BeforeEach
    void ensureBootstrapData() {
        bootstrapDataLoader.run(new DefaultApplicationArguments());
    }

    @Test
    @DisplayName("should register both http://localhost:3000 and https://localhost:8443 as post-logout redirect URIs")
    void shouldRegisterBothPostLogoutRedirectUris() {
        RegisteredClient client = registeredClientRepository.findByClientId(CLIENT_ID);

        assertThat(client).as("OAuth2 client '%s' should be registered", CLIENT_ID).isNotNull();
        assertThat(client.getPostLogoutRedirectUris())
                .as("klabis-web client must allow post-logout redirect to both frontend dev server and backend")
                .containsExactlyInAnyOrder("http://localhost:3000", "https://localhost:8443");
    }

    @Test
    @DisplayName("should NOT register klabis-web-local when local-dev profile is not active")
    void shouldNotRegisterLocalDevClientWithoutLocalDevProfile() {
        RegisteredClient localClient = registeredClientRepository.findByClientId("klabis-web-local");

        assertThat(localClient)
                .as("klabis-web-local must not be registered when local-dev profile is inactive")
                .isNull();
    }
}
