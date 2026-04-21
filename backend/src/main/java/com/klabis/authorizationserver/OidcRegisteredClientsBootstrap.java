package com.klabis.authorizationserver;

import com.klabis.common.bootstrap.BootstrapDataInitializer;
import com.klabis.common.bootstrap.OAuth2ClientProperties;
import com.klabis.common.bootstrap.PasswordGenerator;
import com.klabis.common.users.Authority;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
class OidcRegisteredClientsBootstrap implements BootstrapDataInitializer {
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2ClientProperties clientProperties;
    private static final Logger LOG = LoggerFactory.getLogger(OidcRegisteredClientsBootstrap.class);
    private final PasswordEncoder passwordEncoder;
    private final PasswordGenerator passwordGenerator;
    private final Environment environment;

    OidcRegisteredClientsBootstrap(RegisteredClientRepository registeredClientRepository, OAuth2ClientProperties clientProperties, PasswordEncoder passwordEncoder, PasswordGenerator passwordGenerator, Environment environment) {
        this.registeredClientRepository = registeredClientRepository;
        this.clientProperties = clientProperties;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
        this.environment = environment;
    }

    @Override
    public boolean requiresBootstrap() {
        if (registeredClientRepository.findByClientId(clientProperties.getId()) == null) {
            return true;
        }
        if (isLocalDevProfileActive() && registeredClientRepository.findByClientId(clientProperties.getLocalId()) == null) {
            return true;
        }
        return false;
    }

    @Override
    public void bootstrapData() {
        String clientId = clientProperties.getId();
        String clientUuid = StringUtils.isNotBlank(clientProperties.getUuid()) ? clientProperties.getUuid() : clientId;
        Set<String> redirectUris = Arrays.stream(clientProperties.getRedirectUris().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        Set<String> postLogoutRedirectUris = Arrays.stream(clientProperties.getPostLogoutRedirectUris().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        if (registeredClientRepository.findByClientId(clientId) == null) {
            createPublicWebClient(clientUuid, clientId, redirectUris, postLogoutRedirectUris);
            createOAuth2Client("apispec",
                    "apispec",
                    "apispec",
                    Set.of("https://localhost:8443/swagger-ui/oauth2-redirect.html"),
                    Set.of());
        }

        if (isLocalDevProfileActive() && registeredClientRepository.findByClientId(clientProperties.getLocalId()) == null) {
            warnIfNotLocalhost();
            createLocalDevConfidentialClient();
        }
    }

    private boolean isLocalDevProfileActive() {
        return environment.acceptsProfiles(Profiles.of("local-dev"));
    }

    private void warnIfNotLocalhost() {
        String issuer = environment.getProperty("spring.security.oauth2.authorizationserver.issuer");
        if (StringUtils.isNotBlank(issuer) && !issuer.contains("localhost")) {
            LOG.warn("local-dev profile is active but the authorization server issuer '{}' is not localhost-based. " +
                    "The klabis-web-local confidential client is intended for local development only.", issuer);
        }
    }

    private static final TokenSettings DEFAULT_TOKEN_SETTINGS = TokenSettings.builder()
            .accessTokenTimeToLive(Duration.ofMinutes(5))
            .refreshTokenTimeToLive(Duration.ofHours(24))
            .reuseRefreshTokens(false)
            .build();

    private List<String> resolveScopes() {
        String defaultScopes = String.join(",",
                "openid",
                "profile",
                "email",
                Authority.MEMBERS_SCOPE,
                Authority.EVENTS_SCOPE
        );
        String scopes = StringUtils.isNotBlank(clientProperties.getScopes()) ? clientProperties.getScopes() : defaultScopes;
        return Arrays.stream(scopes.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private void createPublicWebClient(String clientUuid, String clientId, Set<String> redirectUris, Set<String> postLogoutRedirectUris) {
        RegisteredClient c = RegisteredClient.withId(clientUuid)
                .clientId(clientId)
                .clientName("Klabis Web application")
                .clientAuthenticationMethods(items -> items.add(ClientAuthenticationMethod.NONE))
                .authorizationGrantTypes(items -> {
                    items.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                    items.add(AuthorizationGrantType.REFRESH_TOKEN);
                })
                .redirectUris(items -> items.addAll(redirectUris))
                .postLogoutRedirectUris(items -> items.addAll(postLogoutRedirectUris))
                .scopes(items -> items.addAll(resolveScopes()))
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .build())
                .tokenSettings(DEFAULT_TOKEN_SETTINGS)
                .build();

        registeredClientRepository.save(c);

        LOG.info("Created public OAuth2 client with PKCE: {}", clientId);
    }

    private void createOAuth2Client(String clientUuid, String clientId, String clientSecret, Set<String> redirectUris, Set<String> postLogoutRedirectUris) {
        if (StringUtils.isBlank(clientSecret)) {
            LOG.warn("KLABIS_OAUTH2_CLIENT_SECRET not set, generating random secret.");
            clientSecret = passwordGenerator.generateSecurePassword();
            LOG.info("Generated OAuth2 client secret for client '{}': {}", clientId, clientSecret);
        }

        String clientSecretHash = passwordEncoder.encode(clientSecret);

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
                .redirectUris(items -> items.addAll(redirectUris))
                .postLogoutRedirectUris(items -> items.addAll(postLogoutRedirectUris))
                .scopes(items -> items.addAll(resolveScopes()))
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(false)
                        .build())
                .tokenSettings(DEFAULT_TOKEN_SETTINGS)
                .build();

        registeredClientRepository.save(c);

        LOG.info("Created OAuth2 client: {}", clientId);
    }

    private void createLocalDevConfidentialClient() {
        String clientId = clientProperties.getLocalId();
        String clientSecret = clientProperties.getLocalSecret();
        Set<String> redirectUris = Arrays.stream(clientProperties.getLocalRedirectUris().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        String clientSecretHash = passwordEncoder.encode(clientSecret);

        RegisteredClient c = RegisteredClient.withId(clientId + "-id")
                .clientId(clientId)
                .clientSecret(clientSecretHash)
                .clientName("Klabis Web application (local dev)")
                .clientAuthenticationMethods(items -> items.add(ClientAuthenticationMethod.CLIENT_SECRET_POST))
                .authorizationGrantTypes(items -> {
                    items.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                    items.add(AuthorizationGrantType.REFRESH_TOKEN);
                })
                .redirectUris(items -> items.addAll(redirectUris))
                .postLogoutRedirectUris(items -> items.add("http://localhost:3000"))
                .scopes(items -> items.addAll(resolveScopes()))
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .build())
                .tokenSettings(DEFAULT_TOKEN_SETTINGS)
                .build();

        registeredClientRepository.save(c);

        LOG.info("Created local-dev confidential OAuth2 client with PKCE and refresh token: {}", clientId);
    }

}
