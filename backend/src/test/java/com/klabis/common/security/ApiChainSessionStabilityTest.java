package com.klabis.common.security;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.calendar.application.IcalTokenPort;
import com.klabis.common.bootstrap.BootstrapDataLoader;
import com.klabis.common.users.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
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
@DisplayName("API chain does not touch the authorization server session")
class ApiChainSessionStabilityTest {

    private static final String CLIENT_ID = "klabis-web";
    private static final String REDIRECT_URI = "http://localhost:3000/silent-renew.html";
    private static final String CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final String CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BootstrapDataLoader bootstrapDataLoader;

    @Autowired
    private IcalTokenPort icalTokenPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void ensureBootstrapData() {
        bootstrapDataLoader.run(new DefaultApplicationArguments());
    }

    @Test
    @DisplayName("bearer request to /api keeps the session id and sets no cookie")
    void bearerApiCallKeepsSessionId() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String accessToken = obtainAccessToken(session);
        String idBefore = session.getId();

        mockMvc.perform(get("/api").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).session(session))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        assertThat(session.getId()).isEqualTo(idBefore);
    }

    @Test
    @DisplayName("prompt=none still issues a code after a bearer request to /api")
    void promptNoneWorksAfterBearerApiCall() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String accessToken = obtainAccessToken(session);

        mockMvc.perform(get("/api").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).session(session))
                .andExpect(status().isOk());

        mockMvc.perform(authorizeRequest(session).queryParam("prompt", "none"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith(REDIRECT_URI)))
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("code=")))
                .andExpect(header().string(HttpHeaders.LOCATION, not(containsString("error=login_required"))));
    }

    @Test
    @DisplayName("iCal token request keeps the session id")
    void icalTokenRequestKeepsSessionId() throws Exception {
        UUID adminId = jdbcTemplate.queryForObject("SELECT id FROM common.users WHERE user_name = 'admin'", UUID.class);
        String rawToken = icalTokenPort.generateOrRotate(new UserId(adminId)).rawToken();

        MockHttpSession session = new MockHttpSession();
        login(session);
        String idBefore = session.getId();

        mockMvc.perform(get("/ical/my-schedule.ics").param("token", rawToken).session(session))
                .andExpect(status().isOk());

        assertThat(session.getId()).isEqualTo(idBefore);
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

    private void login(MockHttpSession session) throws Exception {
        mockMvc.perform(authorizeRequest(session)).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "admin123")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    private String obtainAccessToken(MockHttpSession session) throws Exception {
        login(session);

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
        JsonNode body = objectMapper.readTree(token.getResponse().getContentAsString());
        return body.get("access_token").asString();
    }
}
