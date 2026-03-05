package com.klabis.common.bootstrap;

import com.klabis.common.users.Authority;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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


@Component
class OidcRegisteredClientsBootstrap implements BootstrapDataInitializer {
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2ClientProperties clientProperties;
    private static final Logger LOG = LoggerFactory.getLogger(OidcRegisteredClientsBootstrap.class);
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;

    OidcRegisteredClientsBootstrap(RegisteredClientRepository registeredClientRepository, OAuth2ClientProperties clientProperties, PasswordEncoder passwordEncoder, PasswordGenerator passwordGenerator) {
        this.registeredClientRepository = registeredClientRepository;
        this.clientProperties = clientProperties;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
    }

    @Override
    public boolean requiresBootstrap() {
        return registeredClientRepository.findByClientId(clientProperties.getId()) == null;
    }

    @Override
    public void bootstrapData() {
        String clientId = clientProperties.getId();
        String clientSecret = clientProperties.getSecret();
        String clientUuid = StringUtils.isNotBlank(clientProperties.getUuid()) ? clientProperties.getUuid() : clientId;
        Set<String> redirectUris = Arrays.stream(clientProperties.getRedirectUris().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        createOAuth2Client(clientUuid, clientId, clientSecret, redirectUris);
        createOAuth2Client("apispec",
                "apispec",
                "apispec",
                Set.of("https://localhost:8443/swagger-ui/oauth2-redirect.html"));
    }

    private void createOAuth2Client(String clientUuid, String clientId, String clientSecret, Set<String> redirectUris) {

        if (StringUtils.isBlank(clientSecret)) {
            LOG.warn("KLABIS_OAUTH2_CLIENT_SECRET not set, generating random secret.");
            clientSecret = passwordGenerator.generateSecurePassword();
            LOG.info("Generated OAuth2 client secret for client '{}': {}", clientId, clientSecret);
        }

        String clientSecretHash = passwordEncoder.encode(clientSecret);

        String defaultScopes = String.join(",",
                "openid",
                "profile",
                "email",
                Authority.MEMBERS_SCOPE,
                Authority.EVENTS_SCOPE
        );

        String scopes = StringUtils.isNotBlank(clientProperties.getScopes()) ? clientProperties.getScopes() : defaultScopes;

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
                .postLogoutRedirectUri(clientProperties.getPostLogoutRedirectUri())
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
