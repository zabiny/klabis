package com.klabis.config;

import com.klabis.users.Authority;
import com.klabis.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bootstrap data loader that populates initial data from environment variables.
 * <p>
 * This component loads bootstrap data (admin user and OAuth2 client) from
 * environment variables instead of hardcoded values in migration files.
 * This prevents credentials from being exposed in version control.
 * <p>
 * Required environment variables (with defaults):
 * - BOOTSTRAP_ADMIN_USERNAME: Admin registration number (default: "admin")
 * - BOOTSTRAP_ADMIN_PASSWORD: Admin password (default: generate random)
 * - BOOTSTRAP_ADMIN_ID: Admin user UUID (default: random UUID)
 * - OAUTH2_CLIENT_ID: OAuth2 client ID (default: "klabis-web")
 * - OAUTH2_CLIENT_SECRET: OAuth2 client secret (default: generate random)
 * - OAUTH2_CLIENT_ID_UUID: OAuth2 client UUID (default: random UUID)
 * - OAUTH2_CLIENT_REDIRECT_URIS: Comma-separated redirect URIs (default: "https://localhost:3000/callback,https://localhost:8443/auth/callback.html")
 */
@Component
public class BootstrapDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapDataLoader.class);

    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final RegisteredClientRepository registeredClientRepository;
    private final UserService userService;

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_OAUTH2_CLIENT_ID = "klabis-web";

    public BootstrapDataLoader(
            PasswordEncoder passwordEncoder,
            Environment environment,
            RegisteredClientRepository registeredClientRepository,
            UserService userService) {
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.registeredClientRepository = registeredClientRepository;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Check if bootstrap data already exists
        if (bootstrapDataExists()) {
            log.info("Bootstrap data already exists, skipping initialization");
            return;
        }

        log.info("Initializing bootstrap data from environment variables");

        try {
            // Load and create bootstrap admin user
            createBootstrapAdminUser();

            // Load and create OAuth2 client
            createOAuth2Clients();

            log.info("Bootstrap data initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize bootstrap data", e);
            // Don't fail the application startup - bootstrap data can be added manually
        }
    }

    private boolean bootstrapDataExists() {
        String username = environment.getProperty("bootstrap.admin.username", DEFAULT_ADMIN_USERNAME);
        return userService.findUserByUsername(username).isPresent();
    }

    private void createBootstrapAdminUser() {
        String username = environment.getProperty("bootstrap.admin.username", DEFAULT_ADMIN_USERNAME);
        String password = environment.getProperty("bootstrap.admin.password");

        // Generate secure random password if not provided
        if (password == null || password.isBlank()) {
            log.warn(
                    "BOOTSTRAP_ADMIN_PASSWORD not set, generating random password. Check logs for the generated password.");
            password = generateSecurePassword();
            log.info("Generated bootstrap admin password for user '{}': {}",
                    username, password);
            log.warn(
                    "Please save this password securely and set BOOTSTRAP_ADMIN_PASSWORD environment variable for future deployments");
        }

        String passwordHash = passwordEncoder.encode(password);

        Set<Authority> authorities = Set.of(Authority.values());

        userService.createActiveUser(username, passwordHash, authorities);

        log.info("Created bootstrap admin user: {} with all authorities", username);
    }

    private void createOAuth2Clients() {
        String clientId = environment.getProperty("oauth2.client.id", DEFAULT_OAUTH2_CLIENT_ID);
        String clientSecret = environment.getProperty("oauth2.client.secret");
        String clientUuid = environment.getProperty("oauth2.client.id.uuid", clientId);
        Set<String> redirectUris = Arrays.stream(environment.getProperty("oauth2.client.redirect-uris",
                "http://localhost:3000/auth/callback,https://localhost:8443/mock/auth/callback.html,https://localhost:8443/auth/callback"
        ).split(",")).map(String::trim).collect(Collectors.toSet());


        createOAuth2Client(clientUuid, clientId, clientSecret, redirectUris);
        createOAuth2Client("apispec", "apispec", "apispec", Set.of("https://localhost:8443/swagger-ui/oauth2-redirect.html"));
    }

    private void createOAuth2Client(String clientUuid, String clientId, String clientSecret, Set<String> redirectUris) {

        // Generate secure random secret if not provided
        if (clientSecret == null || clientSecret.isBlank()) {
            log.warn("OAUTH2_CLIENT_SECRET not set, generating random secret. Check logs for the generated secret.");
            clientSecret = generateSecurePassword();
            log.info("Generated OAuth2 client secret for client '{}': {}", clientId, clientSecret);
            log.warn(
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

        log.info("Created OAuth2 client: {}", clientId);
    }

    private String generateSecurePassword() {
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()-_+=";
        String all = uppercase + lowercase + digits + special;

        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder password = new StringBuilder();

        // Ensure at least one character from each category
        password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // Fill remaining length (20 characters total)
        for (int i = password.length(); i < 20; i++) {
            password.append(all.charAt(random.nextInt(all.length())));
        }

        // Shuffle to avoid predictable pattern
        return shuffleString(password.toString(), random);
    }

    private String shuffleString(String str, java.security.SecureRandom random) {
        char[] chars = str.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}
