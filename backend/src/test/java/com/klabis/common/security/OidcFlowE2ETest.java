package com.klabis.common.security;

import tools.jackson.databind.ObjectMapper;
import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.bootstrap.BootstrapDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
@ApplicationModuleTest(verifyAutomatically = false)
@AutoConfigureMockMvc
@Import(TestApplicationConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "klabis.bootstrap.admin.username=" + OidcFlowE2ETest.ADMIN_USERNAME,
        "klabis.bootstrap.admin.password=" + OidcFlowE2ETest.ADMIN_PASSWORD,
        "klabis.oauth2.client.secret=" + OidcFlowE2ETest.CLIENT_SECRET,
        "logging.level.org.springframework.security=TRACE"
})
@DisplayName("OIDC Complete Flow E2E Test")
@CleanupTestData
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
    static final String CLIENT_SECRET = "test-secret-123";
    static final String ADMIN_USERNAME = "admin";
    static final String ADMIN_PASSWORD = "admin123";
    private static final String REDIRECT_URI = "https://localhost:8443/auth/callback";
    private static final String ISSUER = "https://localhost:8443";

    @BeforeEach
    void ensureBootstrapData() {
        bootstrapDataLoader.run(new DefaultApplicationArguments());
        // Re-create bootstrap data if cleaned by @CleanupTestData from other test classes
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
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith(REDIRECT_URI)))
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("code=")))
                .andReturn();

        String redirectLocation = authorizeResult.getResponse().getHeader(HttpHeaders.LOCATION);
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

        assertThat(idTokenClaims.get(OAuth2TokenClaimNames.SUB)).isEqualTo(ADMIN_USERNAME);
        assertThat(idTokenClaims.get(OAuth2TokenClaimNames.ISS)).isEqualTo(ISSUER);
        assertThat(idTokenClaims.get(KlabisOAuth2ClaimNames.CLAIM_USER_NAME)).isEqualTo(ADMIN_USERNAME);
        assertThat(idTokenClaims).containsKeys(OAuth2TokenClaimNames.SUB, OAuth2TokenClaimNames.ISS, OAuth2TokenClaimNames.AUD, OAuth2TokenClaimNames.EXP, OAuth2TokenClaimNames.IAT);

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
