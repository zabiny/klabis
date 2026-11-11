package com.klabis.authorizationserver;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.bootstrap.BootstrapDataLoader;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.common.users.domain.UserRepository;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardian.application.LegalGuardianProfile;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Set;
import java.util.UUID;

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
@DisplayName("OIDC login of a user with an altUsername")
class OidcAltUsernameLoginTest {

    private static final String CLIENT_ID = "klabis-web";
    private static final String REDIRECT_URI = "http://localhost:3000/silent-renew.html";
    private static final String CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final String CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String PASSWORD = "secret-pass-1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BootstrapDataLoader bootstrapDataLoader;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LegalGuardianPort legalGuardianPort;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String username;
    private String altUsername;

    @BeforeEach
    void setUp() {
        bootstrapDataLoader.run(new DefaultApplicationArguments());
        username = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String unique = UUID.randomUUID().toString().substring(0, 8);
        LegalGuardianProfile profile = legalGuardianPort.register(
                new LegalGuardianPort.NewLegalGuardian("Eva", "Svobodová", "alt." + unique + "@example.com", "+420777111222"));
        UserId id = profile.guardian().getId();
        altUsername = profile.loginName();
        userRepository.save(userRepository.findById(id).orElseThrow().activateWithPassword(passwordEncoder.encode(PASSWORD)));
        userService.replaceUsername(id, username);
    }

    @Test
    @DisplayName("tokens carry the username when logging in with the username")
    void tokensCarryUsernameForUsernameLogin() throws Exception {
        assertTokensCarryUsername(obtainTokens(new MockHttpSession(), username));
    }

    @Test
    @DisplayName("tokens carry the username when logging in with the altUsername")
    void tokensCarryUsernameForAltUsernameLogin() throws Exception {
        assertTokensCarryUsername(obtainTokens(new MockHttpSession(), altUsername));
    }

    private void assertTokensCarryUsername(JsonNode tokens) {
        JsonNode idToken = payload(tokens.get("id_token").asString());
        JsonNode accessToken = payload(tokens.get("access_token").asString());
        assertThat(idToken.get("sub").asString()).isEqualTo(username);
        assertThat(idToken.get("user_name").asString()).isEqualTo(username);
        assertThat(idToken.get("preferred_username").asString()).isEqualTo(username);
        assertThat(accessToken.get("sub").asString()).isEqualTo(username);
        assertThat(accessToken.get("user_name").asString()).isEqualTo(username);
    }

    private JsonNode payload(String jwt) {
        return objectMapper.readTree(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]));
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

    private JsonNode obtainTokens(MockHttpSession session, String login) throws Exception {
        mockMvc.perform(authorizeRequest(session)).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/login")
                        .param("username", login)
                        .param("password", PASSWORD)
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
