package com.klabis.calendar.infrastructure.restapi;

import com.klabis.E2ETest;
import com.klabis.calendar.application.IcalTokenPort;
import com.klabis.common.security.JwtParams;
import com.klabis.common.users.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.klabis.common.security.KlabisMvcRequestBuilders.klabisAuthentication;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GET /api/me/ical-token and POST /api/me/ical-token.
 * <p>
 * Uses full Spring Boot context via @E2ETest. Authentication is simulated via klabisAuthentication().
 */
@E2ETest
@DisplayName("IcalTokenController Integration Tests")
class IcalTokenControllerTest {

    private static final UUID USER_UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UserId USER_ID = new UserId(USER_UUID);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IcalTokenPort icalTokenPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private JwtParams userAuth;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM calendar_feed_token WHERE user_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'");
        jdbcTemplate.execute("DELETE FROM users WHERE id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'");
        jdbcTemplate.execute("""
                INSERT INTO users (id, user_name, password_hash, account_status, created_at, modified_at, version)
                VALUES ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'ZBM8200', 'hash', 'ACTIVE',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                """);
        userAuth = JwtParams.jwtTokenParams("ZBM8200", USER_ID);
    }

    @Nested
    @DisplayName("GET /api/me/ical-token")
    class GetTokenStateTests {

        @Test
        @DisplayName("no token yet → 200 with url=null and no lastSetAt")
        void noToken_returnsNullUrl() throws Exception {
            mockMvc.perform(get("/api/me/ical-token")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$.url").doesNotExist())
                    .andExpect(jsonPath("$.lastSetAt").doesNotExist())
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @DisplayName("token exists → 200 with masked url and lastSetAt")
        void tokenExists_returnsMaskedUrlAndLastSetAt() throws Exception {
            icalTokenPort.generateOrRotate(USER_ID);

            mockMvc.perform(get("/api/me/ical-token")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").isString())
                    .andExpect(jsonPath("$.url", containsString("/ical/my-schedule.ics")))
                    .andExpect(jsonPath("$.url", containsString("token=")))
                    .andExpect(jsonPath("$.url", not(blankString())))
                    .andExpect(jsonPath("$.lastSetAt").isString())
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @DisplayName("token exists → masked url does not contain raw token")
        void tokenExists_maskedUrlDoesNotExposeRawToken() throws Exception {
            String rawToken = icalTokenPort.generateOrRotate(USER_ID).rawToken();

            String responseBody = mockMvc.perform(get("/api/me/ical-token")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            org.assertj.core.api.Assertions.assertThat(responseBody).doesNotContain(rawToken);
        }

        @Test
        @DisplayName("response includes regenerate affordance on self link")
        void response_includesRegenerateAffordance() throws Exception {
            mockMvc.perform(get("/api/me/ical-token")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.generateToken.method", equalToIgnoringCase("POST")));
        }

        @Test
        @DisplayName("unauthenticated request → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/me/ical-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/me/ical-token")
    class GenerateTokenTests {

        @Test
        @DisplayName("no existing token → generates new token, returns full subscribe URL once")
        void noExistingToken_returnsFullUrl() throws Exception {
            mockMvc.perform(post("/api/me/ical-token")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").isString())
                    .andExpect(jsonPath("$.url", containsString("/ical/my-schedule.ics?token=")))
                    .andExpect(jsonPath("$.lastSetAt").isString())
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @DisplayName("existing token → regenerates; old raw token no longer valid")
        void existingToken_regeneratesAndInvalidatesOldToken() throws Exception {
            String oldRawToken = icalTokenPort.generateOrRotate(USER_ID).rawToken();

            mockMvc.perform(post("/api/me/ical-token")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url", not(containsString(oldRawToken))));

            // Old token is invalidated — validate returns empty
            org.assertj.core.api.Assertions.assertThat(icalTokenPort.validate(oldRawToken)).isEmpty();
        }

        @Test
        @DisplayName("POST returns full URL containing the raw token (not masked)")
        void post_returnsFullUrlWithRawToken() throws Exception {
            String responseBody = mockMvc.perform(post("/api/me/ical-token")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // URL must not contain mask characters; must contain a real-looking token (43 base64url chars for 32 bytes)
            org.assertj.core.api.Assertions.assertThat(responseBody)
                    .contains("/ical/my-schedule.ics?token=")
                    .doesNotContain("••••");
        }

        @Test
        @DisplayName("unauthenticated POST → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/me/ical-token"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
