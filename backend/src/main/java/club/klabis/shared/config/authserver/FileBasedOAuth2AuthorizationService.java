package club.klabis.shared.config.authserver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementace OAuth2AuthorizationService, která ukládá autorizační data do JSON souboru.
 * Tato implementace zajišťuje perzistenci OAuth2 autorizací napříč restarty aplikace.
 */
public class FileBasedOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(FileBasedOAuth2AuthorizationService.class);

    private final Map<String, SerializableOAuth2Authorization> authorizations = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ObjectMapper objectMapper;
    private final File storageFile;
    private final RegisteredClientRepository registeredClientRepository;

    /**
     * Konstruktor s výchozím umístěním souboru
     */
    public FileBasedOAuth2AuthorizationService(RegisteredClientRepository registeredClientRepository) {
        this("oauth2-authorizations.json", registeredClientRepository);
    }

    /**
     * Konstruktor s vlastním názvem souboru
     *
     * @param fileName název souboru pro ukládání dat
     */
    public FileBasedOAuth2AuthorizationService(String fileName, RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
        Assert.hasText(fileName, "fileName nesmí být prázdný");
        this.storageFile = new File(fileName);
        this.objectMapper = createObjectMapper();
        loadFromFile();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization nesmí být null");

        lock.writeLock().lock();
        try {
            SerializableOAuth2Authorization serializable = SerializableOAuth2Authorization.from(authorization);
            this.authorizations.put(authorization.getId(), serializable);
            saveToFile();
            logger.debug("Uložena autorizace s ID: {}", authorization.getId());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization nesmí být null");

        lock.writeLock().lock();
        try {
            this.authorizations.remove(authorization.getId());
            saveToFile();
            logger.debug("Odstraněna autorizace s ID: {}", authorization.getId());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    @Nullable
    public OAuth2Authorization findById(String id) {
        Assert.hasText(id, "id nesmí být prázdné");

        lock.readLock().lock();
        try {
            SerializableOAuth2Authorization serializable = this.authorizations.get(id);
            return serializable != null ? serializable.toOAuth2Authorization(registeredClientRepository) : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @Nullable
    public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
        Assert.hasText(token, "token nesmí být prázdný");

        lock.readLock().lock();
        try {
            for (SerializableOAuth2Authorization serializable : this.authorizations.values()) {
                if (hasToken(serializable, token, tokenType)) {
                    return serializable.toOAuth2Authorization(registeredClientRepository);
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    private boolean hasToken(SerializableOAuth2Authorization authorization, String token, @Nullable OAuth2TokenType tokenType) {
        if (tokenType == null) {
            return matchesState(authorization, token) ||
                   matchesAuthorizationCode(authorization, token) ||
                   matchesAccessToken(authorization, token) ||
                   matchesRefreshToken(authorization, token) ||
                   matchesIdToken(authorization, token);
        } else if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
            return matchesState(authorization, token);
        } else if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
            return matchesAuthorizationCode(authorization, token);
        } else if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
            return matchesAccessToken(authorization, token);
        } else if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            return matchesRefreshToken(authorization, token);
        } else if (tokenType.getValue().equals("id_token")) {
            return matchesIdToken(authorization, token);
        }
        return false;
    }

    private boolean matchesState(SerializableOAuth2Authorization authorization, String token) {
        return token.equals(authorization.getAttribute("state"));
    }

    private boolean matchesAuthorizationCode(SerializableOAuth2Authorization authorization, String token) {
        SerializableToken authorizationCode = authorization.getTokens().get(OAuth2ParameterNames.CODE);
        return authorizationCode != null && token.equals(authorizationCode.getTokenValue());
    }

    private boolean matchesAccessToken(SerializableOAuth2Authorization authorization, String token) {
        SerializableToken accessToken = authorization.getTokens().get(OAuth2TokenType.ACCESS_TOKEN.getValue());
        return accessToken != null && token.equals(accessToken.getTokenValue());
    }

    private boolean matchesRefreshToken(SerializableOAuth2Authorization authorization, String token) {
        SerializableToken refreshToken = authorization.getTokens().get(OAuth2TokenType.REFRESH_TOKEN.getValue());
        return refreshToken != null && token.equals(refreshToken.getTokenValue());
    }

    private boolean matchesIdToken(SerializableOAuth2Authorization authorization, String token) {
        SerializableToken idToken = authorization.getTokens().get("id_token");
        return idToken != null && token.equals(idToken.getTokenValue());
    }

    private void saveToFile() {
        try {
            objectMapper.writeValue(storageFile, authorizations);
            logger.debug("Data úspěšně uložena do souboru: {}", storageFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Chyba při ukládání autorizací do souboru: {}", storageFile.getAbsolutePath(), e);
        }
    }

    private void loadFromFile() {
        if (!storageFile.exists()) {
            logger.info("Soubor s autorizacemi neexistuje, bude vytvořen: {}", storageFile.getAbsolutePath());
            return;
        }

        try {
            TypeReference<Map<String, SerializableOAuth2Authorization>> typeRef =
                    new TypeReference<Map<String, SerializableOAuth2Authorization>>() {
                    };
            Map<String, SerializableOAuth2Authorization> loaded = objectMapper.readValue(storageFile, typeRef);
            //loaded.values().forEach(v -> v.parseAttributes(objectMapper));
            this.authorizations.putAll(loaded);
            logger.info("Načteno {} autorizací ze souboru: {}", loaded.size(), storageFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Chyba při načítání autorizací ze souboru: {}", storageFile.getAbsolutePath(), e);
        }
    }

    /**
     * Serializovatelná verze OAuth2Authorization pro JSON ukládání
     */
    public static class SerializableOAuth2Authorization {
        private String id;
        private String registeredClientId;
        private String principalName;
        private String authorizationGrantType;
        private Set<String> authorizedScopes;
        private Map<String, Object> attributes;
        private Map<String, SerializableToken> tokens;

        public SerializableOAuth2Authorization() {
        }

        public static SerializableOAuth2Authorization from(OAuth2Authorization authorization) {
            SerializableOAuth2Authorization serializable = new SerializableOAuth2Authorization();
            serializable.setId(authorization.getId());
            serializable.setRegisteredClientId(authorization.getRegisteredClientId());
            serializable.setPrincipalName(authorization.getPrincipalName());
            serializable.setAuthorizationGrantType(authorization.getAuthorizationGrantType().getValue());
            serializable.setAuthorizedScopes(new HashSet<>(authorization.getAuthorizedScopes()));
            serializable.setAttributes(new HashMap<>(authorization.getAttributes()));

            Map<String, SerializableToken> tokens = new HashMap<>();

            // Authorization Code
            OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                    authorization.getToken(OAuth2AuthorizationCode.class);
            if (authorizationCode != null) {
                tokens.put(OAuth2ParameterNames.CODE, SerializableToken.from(authorizationCode));
            }

            // Access Token
            OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
                    authorization.getToken(OAuth2AccessToken.class);
            if (accessToken != null) {
                tokens.put(OAuth2TokenType.ACCESS_TOKEN.getValue(), SerializableToken.from(accessToken));
            }

            // Refresh Token
            OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
                    authorization.getToken(OAuth2RefreshToken.class);
            if (refreshToken != null) {
                tokens.put(OAuth2TokenType.REFRESH_TOKEN.getValue(), SerializableToken.from(refreshToken));
            }

            // ID Token
            OAuth2Authorization.Token<OidcIdToken> idToken =
                    authorization.getToken(OidcIdToken.class);
            if (idToken != null) {
                tokens.put("id_token", SerializableToken.from(idToken));
            }

            serializable.setTokens(tokens);
            return serializable;
        }

        public OAuth2Authorization toOAuth2Authorization(RegisteredClientRepository registeredClientRepository) {
            RegisteredClient client = registeredClientRepository.findById(registeredClientId);
            Assert.notNull(client, "No OAuth2 client with ID %s was found in repository".formatted(registeredClientId));

            OAuth2Authorization.Builder builder = OAuth2Authorization
                    .withRegisteredClient(client)
                    .id(id)
                    .attributes((attrs) -> attrs.putAll(attributes))
                    .principalName(principalName)
                    .authorizationGrantType(new AuthorizationGrantType(authorizationGrantType))
                    .authorizedScopes(authorizedScopes);

            if (attributes != null) {
                builder.attributes(attrs -> attrs.putAll(attributes));
            }

            if (tokens != null) {
                tokens.forEach((key, token) -> {
                    if (token != null) {
                        builder.token(token.toAbstractOAuth2Token(), metadata -> {
                            if (token.getMetadata() != null) {
                                metadata.putAll(token.getMetadata());
                            }
                        });
                    }
                });
            }

            return builder.build();
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getRegisteredClientId() {
            return registeredClientId;
        }

        public void setRegisteredClientId(String registeredClientId) {
            this.registeredClientId = registeredClientId;
        }

        public String getPrincipalName() {
            return principalName;
        }

        public void setPrincipalName(String principalName) {
            this.principalName = principalName;
        }

        public String getAuthorizationGrantType() {
            return authorizationGrantType;
        }

        public void setAuthorizationGrantType(String authorizationGrantType) {
            this.authorizationGrantType = authorizationGrantType;
        }

        public Set<String> getAuthorizedScopes() {
            return authorizedScopes;
        }

        public void setAuthorizedScopes(Set<String> authorizedScopes) {
            this.authorizedScopes = authorizedScopes;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        public Map<String, SerializableToken> getTokens() {
            return tokens;
        }

        public void setTokens(Map<String, SerializableToken> tokens) {
            this.tokens = tokens;
        }

        public Object getAttribute(String name) {
            return this.attributes != null ? this.attributes.get(name) : null;
        }
    }

    /**
     * Serializovatelná verze tokenu
     */
    public static class SerializableToken {
        private String tokenValue;
        private Instant issuedAt;
        private Instant expiresAt;
        private Map<String, Object> metadata;
        private String tokenType;

        public SerializableToken() {
        }

        public static SerializableToken from(OAuth2Authorization.Token<?> token) {
            SerializableToken serializable = new SerializableToken();
            serializable.setTokenValue(token.getToken().getTokenValue());
            serializable.setIssuedAt(token.getToken().getIssuedAt());
            serializable.setExpiresAt(token.getToken().getExpiresAt());
            serializable.setMetadata(new HashMap<>(token.getMetadata()));

            if (token.getToken() instanceof OAuth2AuthorizationCode) {
                serializable.setTokenType("authorization_code");
            } else if (token.getToken() instanceof OAuth2AccessToken) {
                serializable.setTokenType("access_token");
            } else if (token.getToken() instanceof OAuth2RefreshToken) {
                serializable.setTokenType("refresh_token");
            } else if (token.getToken() instanceof OidcIdToken) {
                serializable.setTokenType("id_token");
            }

            return serializable;
        }

        public AbstractOAuth2Token toAbstractOAuth2Token() {
            if ("authorization_code".equals(tokenType)) {
                return new OAuth2AuthorizationCode(tokenValue, issuedAt, expiresAt);
            } else if ("access_token".equals(tokenType)) {
                return new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        tokenValue,
                        issuedAt,
                        expiresAt
                );
            } else if ("refresh_token".equals(tokenType)) {
                return new OAuth2RefreshToken(tokenValue, issuedAt, expiresAt);
            } else if ("id_token".equals(tokenType)) {
                return new OidcIdToken(
                        tokenValue,
                        issuedAt,
                        expiresAt,
                        (Map<String, Object>) metadata.get(OAuth2Authorization.Token.CLAIMS_METADATA_NAME)
                );
            }
            throw new IllegalStateException("Neznámý typ tokenu: " + tokenType);
        }

        // Getters and Setters
        public String getTokenValue() {
            return tokenValue;
        }

        public void setTokenValue(String tokenValue) {
            this.tokenValue = tokenValue;
        }

        public Instant getIssuedAt() {
            return issuedAt;
        }

        public void setIssuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }
    }
}
