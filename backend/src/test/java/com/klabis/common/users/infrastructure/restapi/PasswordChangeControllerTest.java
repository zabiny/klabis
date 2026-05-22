package com.klabis.common.users.infrastructure.restapi;

import com.klabis.E2ETest;
import com.klabis.common.security.JwtParams;
import com.klabis.common.users.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.klabis.common.security.KlabisMvcRequestBuilders.klabisAuthentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@E2ETest
@DisplayName("PasswordChangeController integration tests")
class PasswordChangeControllerTest {

    private static final UUID USER_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UserId USER_ID = new UserId(USER_UUID);
    private static final String CURRENT_PASSWORD = "Current$Pass1!word";
    private static final String VALID_NEW_PASSWORD = "NewSecure!Pass1word";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private JwtParams userAuth;

    @BeforeEach
    void setUp() {
        String hash = passwordEncoder.encode(CURRENT_PASSWORD);
        jdbcTemplate.execute("DELETE FROM users WHERE id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'");
        jdbcTemplate.update("""
                INSERT INTO users (id, user_name, password_hash, account_status, created_at, modified_at, version)
                VALUES (?, 'ZBM8300', ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                """, USER_UUID, hash);
        userAuth = JwtParams.jwtTokenParams("ZBM8300", USER_ID);
    }

    @Nested
    @DisplayName("POST /api/me/password-change")
    class ChangePasswordTests {

        @Test
        @DisplayName("correct current password and valid new password → 204")
        void successPath_returns204() throws Exception {
            mockMvc.perform(post("/api/me/password-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"%s","newPassword":"%s"}
                                    """.formatted(CURRENT_PASSWORD, VALID_NEW_PASSWORD))
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("new password accepted — subsequent login with new password works (hash updated)")
        void passwordUpdatedInDatabase() throws Exception {
            mockMvc.perform(post("/api/me/password-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"%s","newPassword":"%s"}
                                    """.formatted(CURRENT_PASSWORD, VALID_NEW_PASSWORD))
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isNoContent());

            String storedHash = jdbcTemplate.queryForObject(
                    "SELECT password_hash FROM users WHERE id = ?", String.class, USER_UUID);
            org.assertj.core.api.Assertions.assertThat(
                    passwordEncoder.matches(VALID_NEW_PASSWORD, storedHash)).isTrue();
        }

        @Test
        @DisplayName("wrong current password → 400 with error detail")
        void wrongCurrentPassword_returns400() throws Exception {
            mockMvc.perform(post("/api/me/password-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"WrongPassword1!","newPassword":"%s"}
                                    """.formatted(VALID_NEW_PASSWORD))
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Current password is incorrect"));
        }

        @Test
        @DisplayName("wrong current password → stored password not changed")
        void wrongCurrentPassword_doesNotChangeStoredPassword() throws Exception {
            String hashBefore = jdbcTemplate.queryForObject(
                    "SELECT password_hash FROM users WHERE id = ?", String.class, USER_UUID);

            mockMvc.perform(post("/api/me/password-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"WrongPassword1!","newPassword":"%s"}
                                    """.formatted(VALID_NEW_PASSWORD))
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isBadRequest());

            String hashAfter = jdbcTemplate.queryForObject(
                    "SELECT password_hash FROM users WHERE id = ?", String.class, USER_UUID);
            org.assertj.core.api.Assertions.assertThat(hashAfter).isEqualTo(hashBefore);
        }

        @Test
        @DisplayName("new password too short (fails complexity) → 400")
        void weakNewPassword_tooShort_returns400() throws Exception {
            mockMvc.perform(post("/api/me/password-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"%s","newPassword":"Short1!"}
                                    """.formatted(CURRENT_PASSWORD))
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").isString());
        }

        @Test
        @DisplayName("new password missing uppercase → 400")
        void weakNewPassword_noUppercase_returns400() throws Exception {
            mockMvc.perform(post("/api/me/password-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"%s","newPassword":"nouppercase1!abc"}
                                    """.formatted(CURRENT_PASSWORD))
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").isString());
        }

        @Test
        @DisplayName("new password missing special character → 400")
        void weakNewPassword_noSpecialChar_returns400() throws Exception {
            mockMvc.perform(post("/api/me/password-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"%s","newPassword":"NoSpecialChar123"}
                                    """.formatted(CURRENT_PASSWORD))
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").isString());
        }

        @Test
        @DisplayName("unauthenticated request → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/me/password-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"%s","newPassword":"%s"}
                                    """.formatted(CURRENT_PASSWORD, VALID_NEW_PASSWORD)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("blank currentPassword field → 400")
        void blankCurrentPassword_returns400() throws Exception {
            mockMvc.perform(post("/api/me/password-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"","newPassword":"%s"}
                                    """.formatted(VALID_NEW_PASSWORD))
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("blank newPassword field → 400")
        void blankNewPassword_returns400() throws Exception {
            mockMvc.perform(post("/api/me/password-change")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"currentPassword":"%s","newPassword":""}
                                    """.formatted(CURRENT_PASSWORD))
                            .with(klabisAuthentication(userAuth)))
                    .andExpect(status().isBadRequest());
        }
    }
}
