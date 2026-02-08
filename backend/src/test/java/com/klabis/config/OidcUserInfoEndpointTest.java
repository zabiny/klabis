package com.klabis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for OpenID Connect UserInfo endpoint.
 * <p>
 * Tests the /userinfo endpoint provided by Spring Authorization Server:
 * <ul>
 *   <li>Valid access tokens with openid scope grant access to user information</li>
 *   <li>Expired tokens are rejected with 401</li>
 *   <li>Tokens without openid scope are rejected</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestApplicationConfiguration.class)
@ActiveProfiles("test")
@CleanupTestData
@TestPropertySource(properties = {
        "bootstrap.admin.username=admin",
        "bootstrap.admin.password=admin123",
        "oauth2.client.secret=test-secret-123"
})
@DisplayName("OIDC UserInfo Endpoint Tests")
class OidcUserInfoEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private BootstrapDataLoader bootstrapDataLoader;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String CLIENT_ID = "klabis-web";
    private static final String CLIENT_SECRET = "test-secret-123";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String REDIRECT_URI = "https://localhost:8443/auth/callback.html";

    @BeforeEach
    void ensureBootstrapData() {
        // Re-create bootstrap data if cleaned by @CleanupTestData from other test classes
        bootstrapDataLoader.run(new DefaultApplicationArguments());
        RegisteredClient client = registeredClientRepository.findByClientId(CLIENT_ID);
        assertThat(client)
                .as("OAuth2 client '%s' should be registered", CLIENT_ID)
                .isNotNull();
        assertThat(client.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(client.getScopes()).containsExactlyInAnyOrder("openid",
                "MEMBERS:CREATE",
                "MEMBERS:UPDATE",
                "MEMBERS:DELETE",
                "MEMBERS:READ");
        // Don't compare hashed password - BCrypt generates different hash each time
        assertThat(passwordEncoder.matches(CLIENT_SECRET, client.getClientSecret())).isTrue();
    }

    @Test
    @DisplayName("should return user info with valid access token")
    void shouldReturnUserInfoWithValidAccessToken() throws Exception {
        // GIVEN: A valid access token obtained through authorization code flow with openid scope
        String accessToken = obtainAccessTokenWithScope("openid");

        // WHEN: Calling UserInfo endpoint with Bearer token
        // THEN: Should return 200 OK with user claims including Member profile data
        // Note: Admin user has no linked Member entity, so only sub and registrationNumber are returned
        mockMvc.perform(
                        get("/userinfo")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.registrationNumber").value(ADMIN_USERNAME));
        // firstName and lastName are not returned for admin user (no linked Member entity)
    }

    @Test
    @DisplayName("should reject expired access token")
    void shouldRejectExpiredAccessToken() throws Exception {
        // GIVEN: A manually created expired JWT token
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("https://localhost:8443")
                .subject(ADMIN_USERNAME)
                .issuedAt(now.minus(2, ChronoUnit.HOURS))
                .expiresAt(now.minus(1, ChronoUnit.HOURS))
                .claim("scope", "openid")
                .build();

        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        // WHEN: Calling UserInfo endpoint with expired token
        // THEN: Should return 401 Unauthorized
        mockMvc.perform(
                        get("/userinfo")
                                .header("Authorization", "Bearer " + expiredToken)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should reject request without openid scope")
    void shouldRejectRequestWithoutOpenidScope() throws Exception {
        // GIVEN: An access token obtained without openid scope
        String accessToken = obtainAccessTokenWithoutOpenidScope();

        // WHEN: Calling UserInfo endpoint with token lacking openid scope
        // THEN: Should return 403 Forbidden (insufficient scope)
        mockMvc.perform(
                        get("/userinfo")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isForbidden());
    }

    /**
     * Obtains an access token via the authorization code flow with the specified scope.
     */
    @SuppressWarnings("unchecked")
    private String obtainAccessTokenWithScope(String scope) throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Step 1: Initiate authorization - redirects to login
        mockMvc.perform(
                        get("/oauth2/authorize")
                                .queryParam("response_type", "code")
                                .queryParam("client_id", CLIENT_ID)
                                .queryParam("scope", scope)
                                .queryParam("redirect_uri", REDIRECT_URI)
                                .queryParam("state", "test-state")
                                .session(session)
                )
                .andExpect(status().is3xxRedirection());

        // Step 2: Authenticate user
        mockMvc.perform(
                        post("/login")
                                .param("username", ADMIN_USERNAME)
                                .param("password", ADMIN_PASSWORD)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection());

        // Step 3: Complete authorization - get code
        MvcResult authorizeResult = mockMvc.perform(
                        get("/oauth2/authorize")
                                .queryParam("response_type", "code")
                                .queryParam("client_id", CLIENT_ID)
                                .queryParam("scope", scope)
                                .queryParam("redirect_uri", REDIRECT_URI)
                                .queryParam("state", "test-state")
                                .session(session)
                )
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectLocation = authorizeResult.getResponse().getHeader("Location");
        String authorizationCode = extractAuthorizationCode(redirectLocation);

        ensureBootstrapData();
        // Step 4: Exchange code for tokens
        MvcResult tokenResult = mockMvc.perform(
                        post("/oauth2/token")
                                .param("grant_type", "authorization_code")
                                .param("code", authorizationCode)
                                .param("redirect_uri", REDIRECT_URI)
                                .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                )
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = tokenResult.getResponse().getContentAsString();
        Map<String, Object> tokenResponse = objectMapper.readValue(responseBody, Map.class);
        return (String) tokenResponse.get("access_token");
    }

    /**
     * Obtains an access token via client_credentials grant (no openid scope possible).
     * Client credentials tokens do not have user-level openid scope.
     */
    @SuppressWarnings("unchecked")
    private String obtainAccessTokenWithoutOpenidScope() throws Exception {
        MvcResult tokenResult = mockMvc.perform(
                        post("/oauth2/token")
                                .param("grant_type", "client_credentials")
                                .param("scope", "MEMBERS:READ")
                                .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                )
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = tokenResult.getResponse().getContentAsString();
        Map<String, Object> tokenResponse = objectMapper.readValue(responseBody, Map.class);
        return (String) tokenResponse.get("access_token");
    }

    private String extractAuthorizationCode(String redirectUri) {
        int codeStart = redirectUri.indexOf("code=") + 5;
        int codeEnd = redirectUri.indexOf("&", codeStart);
        if (codeEnd == -1) {
            return redirectUri.substring(codeStart);
        }
        return redirectUri.substring(codeStart, codeEnd);
    }
}
