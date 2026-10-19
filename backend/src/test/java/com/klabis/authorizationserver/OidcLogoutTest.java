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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestApplicationConfiguration.class)
@ActiveProfiles({"test", "test-bootstrap"})
@TestPropertySource(properties = {
        "klabis.bootstrap.admin.username=admin",
        "klabis.bootstrap.admin.password=admin123",
        "klabis.oauth2.client.secret=test-secret-123"
})
@CleanupTestData
@DisplayName("OIDC RP-initiated logout")
class OidcLogoutTest {

    private static final String CLIENT_ID = "klabis-web";
    private static final String REDIRECT_URI = "http://localhost:3000/silent-renew.html";
    private static final String POST_LOGOUT_REDIRECT_URI = "http://localhost:3000";
    private static final String CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final String CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BootstrapDataLoader bootstrapDataLoader;

    @BeforeEach
    void ensureBootstrapData() {
        bootstrapDataLoader.run(new DefaultApplicationArguments());
    }

    @Test
    @DisplayName("logout with a live authorization server session redirects to post_logout_redirect_uri")
    void logoutWithLiveSessionRedirectsBack() throws Exception {
        MockHttpSession session = new MockHttpSession();
        JsonNode tokens = obtainTokens(session);

        // Same-origin SPA traffic sends the AS session cookie along; it must not break the sid check at logout
        mockMvc.perform(get("/api")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.get("access_token").asString())
                        .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/connect/logout")
                        .queryParam("id_token_hint", tokens.get("id_token").asString())
                        .queryParam("client_id", CLIENT_ID)
                        .queryParam("post_logout_redirect_uri", POST_LOGOUT_REDIRECT_URI)
                        .queryParam("state", "logout-state")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, POST_LOGOUT_REDIRECT_URI + "?state=logout-state"));

        assertThat(session.isInvalid()).isTrue();
    }

    private MockHttpServletRequestBuilder authorizeRequest(MockHttpSession session) {
        return get("/oauth2/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", CLIENT_ID)
                .queryParam("scope", "openid")
                .queryParam("redirect_uri", REDIRECT_URI)
                .queryParam("state", "state")
                .queryParam("code_challenge", CODE_CHALLENGE)
                .queryParam("code_challenge_method", "S256")
                .session(session);
    }

    private JsonNode obtainTokens(MockHttpSession session) throws Exception {
        mockMvc.perform(authorizeRequest(session)).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "admin123")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        MvcResult authorized = mockMvc.perform(authorizeRequest(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("code=")))
                .andReturn();
        String location = authorized.getResponse().getHeader(HttpHeaders.LOCATION);
        String code = UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("code");

        MvcResult token = mockMvc.perform(post("/oauth2/token")
                        .param("grant_type", "authorization_code")
                        .param("client_id", CLIENT_ID)
                        .param("code", code)
                        .param("redirect_uri", REDIRECT_URI)
                        .param("code_verifier", CODE_VERIFIER))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(token.getResponse().getContentAsString());
    }
}
