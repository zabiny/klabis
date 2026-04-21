package com.klabis.authorizationserver;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.bootstrap.BootstrapDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that the authorization server filter chain handles {@code prompt=none}
 * per the OpenID Connect specification (section 3.1.2.1).
 *
 * <p>When {@code prompt=none} is set, the authorization server MUST NOT redirect to a login
 * page — it must return a {@code login_required} error to the {@code redirect_uri} instead.
 * This is critical for silent token renewal via an iframe, which cannot display any UI.
 *
 * <p>When {@code prompt=none} is absent, normal behavior applies: unauthenticated TEXT_HTML
 * requests are redirected to {@code /login}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestApplicationConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "klabis.bootstrap.admin.username=admin",
        "klabis.bootstrap.admin.password=admin123",
        "klabis.oauth2.client.secret=test-secret-123"
})
@CleanupTestData
@DisplayName("Authorization server: prompt=none handling")
class AuthorizationServerPromptNoneTest {

    private static final String CLIENT_ID = "klabis-web";
    private static final String REDIRECT_URI = "http://localhost:3000/silent-renew.html";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapDataLoader bootstrapDataLoader;

    @BeforeEach
    void ensureBootstrapData() {
        bootstrapDataLoader.run(new DefaultApplicationArguments());
    }

    @Test
    @DisplayName("unauthenticated /oauth2/authorize with prompt=none must NOT redirect to /login")
    void shouldNotRedirectToLoginWhenPromptNone() throws Exception {
        // Per OIDC spec, prompt=none requires login_required error sent to redirect_uri,
        // not a redirect to a login page (which would break iframe-based silent renew).
        mockMvc.perform(
                        get("/oauth2/authorize")
                                .accept(MediaType.TEXT_HTML)
                                .queryParam("response_type", "code")
                                .queryParam("client_id", CLIENT_ID)
                                .queryParam("scope", "openid")
                                .queryParam("redirect_uri", REDIRECT_URI)
                                .queryParam("state", "silent-renew-state")
                                .queryParam("prompt", "none")
                                .queryParam("code_challenge", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                                .queryParam("code_challenge_method", "S256")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith(REDIRECT_URI)))
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("error=login_required")))
                .andExpect(header().string(HttpHeaders.LOCATION, not(containsString("/login"))));
    }

    @Test
    @DisplayName("unauthenticated /oauth2/authorize without prompt=none must redirect to /login")
    void shouldRedirectToLoginWhenNoPromptNone() throws Exception {
        mockMvc.perform(
                        get("/oauth2/authorize")
                                .accept(MediaType.TEXT_HTML)
                                .queryParam("response_type", "code")
                                .queryParam("client_id", CLIENT_ID)
                                .queryParam("scope", "openid")
                                .queryParam("redirect_uri", REDIRECT_URI)
                                .queryParam("state", "regular-state")
                                .queryParam("code_challenge", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                                .queryParam("code_challenge_method", "S256")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/login")));
    }

    @Test
    @DisplayName("prompt=none with unregistered redirect_uri must return HTTP 400, not redirect to attacker URL")
    void shouldReturn400WhenPromptNoneWithUnregisteredRedirectUri() throws Exception {
        // An attacker attempts to use this server as an open redirector by providing an
        // arbitrary redirect_uri that is not registered for the client.
        mockMvc.perform(
                        get("/oauth2/authorize")
                                .accept(MediaType.TEXT_HTML)
                                .queryParam("response_type", "code")
                                .queryParam("client_id", CLIENT_ID)
                                .queryParam("scope", "openid")
                                .queryParam("redirect_uri", "https://evil.example/phish")
                                .queryParam("state", "attacker-state")
                                .queryParam("prompt", "none")
                                .queryParam("code_challenge", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                                .queryParam("code_challenge_method", "S256")
                )
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }

    @Test
    @DisplayName("prompt=none with unknown client_id must return HTTP 400, not redirect")
    void shouldReturn400WhenPromptNoneWithUnknownClientId() throws Exception {
        mockMvc.perform(
                        get("/oauth2/authorize")
                                .accept(MediaType.TEXT_HTML)
                                .queryParam("response_type", "code")
                                .queryParam("client_id", "unknown-client")
                                .queryParam("scope", "openid")
                                .queryParam("redirect_uri", REDIRECT_URI)
                                .queryParam("state", "some-state")
                                .queryParam("prompt", "none")
                                .queryParam("code_challenge", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                                .queryParam("code_challenge_method", "S256")
                )
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }
}
