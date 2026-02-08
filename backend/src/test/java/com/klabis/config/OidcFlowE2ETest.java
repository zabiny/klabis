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
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test for complete OpenID Connect flow.
 * <p>
 * This test verifies the full OIDC authentication flow from discovery through
 * token issuance and UserInfo retrieval:
 * <ol>
 *   <li>Discovery: GET /.well-known/openid-configuration</li>
 *   <li>Authorization: GET /oauth2/authorize with openid scope</li>
 *   <li>Authentication: POST /login with credentials</li>
 *   <li>Token Exchange: POST /oauth2/token with authorization code</li>
 *   <li>UserInfo: GET /userinfo with access token</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestApplicationConfiguration.class)
@ActiveProfiles("test")
@CleanupTestData
@TestPropertySource(properties = {
        "bootstrap.admin.username=admin",
        "bootstrap.admin.password=admin123",
        "oauth2.client.secret=test-secret-123",
        "logging.level.org.springframework.security=TRACE"
})
@DisplayName("OIDC Complete Flow E2E Test")
class OidcFlowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private BootstrapDataLoader bootstrapDataLoader;

    private static final String CLIENT_ID = "klabis-web";
    private static final String CLIENT_SECRET = "test-secret-123";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String REDIRECT_URI = "https://localhost:8443/auth/callback.html";
    private static final String ISSUER = "https://localhost:8443";

    @BeforeEach
    void ensureBootstrapData() {
        // Re-create bootstrap data if cleaned by @CleanupTestData from other test classes
        bootstrapDataLoader.run(new DefaultApplicationArguments());
        assertThat(registeredClientRepository.findByClientId(CLIENT_ID))
                .as("OAuth2 client '%s' should be registered", CLIENT_ID)
                .isNotNull();
    }

    @Test
    @DisplayName("should complete OIDC flow: discovery -> authorize -> token -> userinfo")
    @SuppressWarnings("unchecked")
    void shouldCompleteOidcFlow_authorize_token_userinfo() throws Exception {
        // STEP 1: Discovery - verify OIDC metadata is available
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value(ISSUER))
                .andExpect(jsonPath("$.authorization_endpoint").value(ISSUER + "/oauth2/authorize"))
                .andExpect(jsonPath("$.token_endpoint").value(ISSUER + "/oauth2/token"))
                .andExpect(jsonPath("$.userinfo_endpoint").value(ISSUER + "/userinfo"))
                .andExpect(jsonPath("$.jwks_uri").value(ISSUER + "/oauth2/jwks"));

        // STEP 2: Authorization Code Flow
        MockHttpSession session = new MockHttpSession();

        // Step 2a: Initiate authorization - should redirect to login
        mockMvc.perform(
                        get("/oauth2/authorize")
                                .queryParam("response_type", "code")
                                .queryParam("client_id", CLIENT_ID)
                                .queryParam("scope", "openid")
                                .queryParam("redirect_uri", REDIRECT_URI)
                                .queryParam("state", "test-state")
                                .session(session)
                )
                .andDo(print())
                .andExpect(status().is3xxRedirection());

        // Step 2b: Authenticate user via login form
        mockMvc.perform(
                        post("/login")
                                .param("username", ADMIN_USERNAME)
                                .param("password", ADMIN_PASSWORD)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection());

        // Step 2c: Complete authorization with authenticated session
        MvcResult authorizeResult = mockMvc.perform(
                        get("/oauth2/authorize")
                                .queryParam("response_type", "code")
                                .queryParam("client_id", CLIENT_ID)
                                .queryParam("scope", "openid")
                                .queryParam("redirect_uri", REDIRECT_URI)
                                .queryParam("state", "test-state")
                                .session(session)
                )
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectLocation = authorizeResult.getResponse().getHeader("Location");
        assertThat(redirectLocation)
                .isNotNull()
                .contains("code=")
                .startsWith(REDIRECT_URI);

        String authorizationCode = extractAuthorizationCode(redirectLocation);

        // STEP 3: Exchange code for tokens
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(
                (CLIENT_ID + ":" + CLIENT_SECRET).getBytes()
        );

        MvcResult tokenResult = mockMvc.perform(
                        post("/oauth2/token")
                                .param("grant_type", "authorization_code")
                                .param("code", authorizationCode)
                                .param("redirect_uri", REDIRECT_URI)
                                .header("Authorization", basicAuth)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.id_token").exists())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andReturn();

        String tokenResponse = tokenResult.getResponse().getContentAsString();
        Map<String, Object> tokens = objectMapper.readValue(tokenResponse, Map.class);

        String accessToken = (String) tokens.get("access_token");
        String idToken = (String) tokens.get("id_token");

        assertThat(accessToken).isNotBlank();
        assertThat(idToken).isNotBlank();

        // Verify ID token structure and claims
        String[] idTokenParts = idToken.split("\\.");
        assertThat(idTokenParts).hasSize(3);

        String payload = new String(Base64.getUrlDecoder().decode(idTokenParts[1]));
        Map<String, Object> idTokenClaims = objectMapper.readValue(payload, Map.class);

        assertThat(idTokenClaims.get("sub")).isEqualTo(ADMIN_USERNAME);
        assertThat(idTokenClaims.get("iss")).isEqualTo(ISSUER);
        assertThat(idTokenClaims.get("registrationNumber")).isEqualTo(ADMIN_USERNAME);
        assertThat(idTokenClaims).containsKeys("sub", "iss", "aud", "exp", "iat");

        // STEP 4: Call UserInfo endpoint with access token
        mockMvc.perform(
                        get("/userinfo")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value(ADMIN_USERNAME));
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
