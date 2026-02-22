package com.klabis.common.bootstrap;

import com.klabis.users.Authority;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BootstrapDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapDataLoader.class);

    private final List<BootstrapDataInitializer> bootstrapDataInitializers;

    public BootstrapDataLoader(List<BootstrapDataInitializer> bootstrapDataInitializers) {
        this.bootstrapDataInitializers = bootstrapDataInitializers;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        bootstrapDataInitializers.forEach(this::bootstrapData);
    }

    /**
     * @param bootstrapDataInitializer
     * @return true if data are successfully set up
     */
    private boolean bootstrapData(BootstrapDataInitializer bootstrapDataInitializer) {
        try {
            if (bootstrapDataInitializer.requiresBootstrap()) {
                log.info("Running bootstrap data initializer {}", bootstrapDataInitializer.getClass().getSimpleName());
                bootstrapDataInitializer.bootstrapData();
            } else {
                log.trace("Bootstrap data from initializer {} are completed, skipping",
                        bootstrapDataInitializer.getClass().getSimpleName());
            }
            return true;
        } catch (Exception ex) {
            log.warn("Failed to initialize bootstrap data using %s".formatted(bootstrapDataInitializer.getClass()
                    .getCanonicalName()), ex);
            return false;
        }
    }
}


/**
 * Required environment variables (with defaults):
 * - OAUTH2_CLIENT_ID: OAuth2 client ID (default: "klabis-web")
 * - OAUTH2_CLIENT_SECRET: OAuth2 client secret (default: generate random)
 * - OAUTH2_CLIENT_ID_UUID: OAuth2 client UUID (default: random UUID)
 * - OAUTH2_CLIENT_REDIRECT_URIS: Comma-separated redirect URIs (default: "https://localhost:3000/callback,https://localhost:8443/auth/callback.html")
 */
@Component
class OidcRegisteredClientsBootstrap implements BootstrapDataInitializer {
    private final RegisteredClientRepository registeredClientRepository;
    private final Environment environment;
    private static final Logger LOG = LoggerFactory.getLogger(OidcRegisteredClientsBootstrap.class);
    private final PasswordEncoder passwordEncoder;
    private static final String DEFAULT_OAUTH2_CLIENT_ID = "klabis-web";
    private final PasswordGenerator passwordGenerator;

    @Value("${oauth2.client.id:" + DEFAULT_OAUTH2_CLIENT_ID + "}")
    private String CLIENT_ID;

    OidcRegisteredClientsBootstrap(RegisteredClientRepository registeredClientRepository, Environment environment, PasswordEncoder passwordEncoder, PasswordGenerator passwordGenerator) {
        this.registeredClientRepository = registeredClientRepository;
        this.environment = environment;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
    }

    @Override
    public boolean requiresBootstrap() {
        return registeredClientRepository.findByClientId(DEFAULT_OAUTH2_CLIENT_ID) == null;
    }

    @Override
    public void bootstrapData() {
        String clientId = environment.getProperty("oauth2.client.id", DEFAULT_OAUTH2_CLIENT_ID);
        String clientSecret = environment.getProperty("oauth2.client.secret");
        String clientUuid = environment.getProperty("oauth2.client.id.uuid", clientId);
        Set<String> redirectUris = Arrays.stream(environment.getProperty("oauth2.client.redirect-uris",
                "http://localhost:3000/auth/callback,https://localhost:8443/mock/auth/callback.html,https://localhost:8443/auth/callback"
        ).split(",")).map(String::trim).collect(Collectors.toSet());


        createOAuth2Client(clientUuid, clientId, clientSecret, redirectUris);
        createOAuth2Client("apispec",
                "apispec",
                "apispec",
                Set.of("https://localhost:8443/swagger-ui/oauth2-redirect.html"));
    }

    private void createOAuth2Client(String clientUuid, String clientId, String clientSecret, Set<String> redirectUris) {

        // Generate secure random secret if not provided
        if (StringUtils.isBlank(clientSecret)) {
            LOG.warn("OAUTH2_CLIENT_SECRET not set, generating random secret. Check logs for the generated secret.");
            clientSecret = passwordGenerator.generateSecurePassword();
            LOG.info("Generated OAuth2 client secret for client '{}': {}", clientId, clientSecret);
            LOG.warn(
                    "Please save this secret securely and set OAUTH2_CLIENT_SECRET environment variable for future deployments");
        }

        String clientSecretHash = passwordEncoder.encode(clientSecret);

        // Get scopes from environment or use default
        // Note: Using Authority enum values for type safety, plus 'openid' for OIDC support
        String defaultScopes = String.join(",",
                "openid",  // OpenID Connect scope
                "profile", // OIDC standard scope for profile claims
                "email",   // OIDC standard scope for email claims
                Authority.MEMBERS_SCOPE,
                Authority.EVENTS_SCOPE
        );

        String scopes = environment.getProperty(
                "oauth2.client.scopes",
                defaultScopes
        );

        RegisteredClient c = RegisteredClient.withId(clientUuid)
                .clientId(clientId)
                .clientSecret(clientSecretHash)
                .clientName("Klabis Web application")
                .clientAuthenticationMethods(items -> {
                    items.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                    items.add(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                })
                .authorizationGrantTypes(items -> {
                    items.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                    items.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
                    items.add(AuthorizationGrantType.REFRESH_TOKEN);
                })
                .redirectUris(items -> {
                    items.addAll(redirectUris);
                })
                .postLogoutRedirectUri(environment.getProperty("oauth2.client.post-logout-redirect-uris",
                        "https://localhost:8443"))
                .scopes(items -> items.addAll(Arrays.stream(scopes.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList())))
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .refreshTokenTimeToLive(Duration.ofHours(24))
                        .reuseRefreshTokens(false)
                        .build())
                .build();

        registeredClientRepository.save(c);

        LOG.info("Created OAuth2 client: {}", clientId);
    }

}
