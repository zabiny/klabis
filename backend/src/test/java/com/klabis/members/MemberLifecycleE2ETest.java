package com.klabis.members;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.E2ETest;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.email.EmailProperties;
import com.klabis.common.email.EmailService;
import com.klabis.common.email.LoggingEmailService;
import com.klabis.common.users.Authority;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.klabis.common.security.JwtParams.jwtTokenParams;
import static com.klabis.common.security.KlabisMvcRequestBuilders.klabisAuthentication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test for complete Member aggregate lifecycle.
 * <p>
 * This test verifies the entire member lifecycle from registration through termination:
 * <ul>
 *   <li>Member registration</li>
 *   <li>Member presence in list (soft delete verification)</li>
 *   <li>Member retrieval and verification</li>
 *   <li>Email token retrieval (simulated via LoggingEmailService)</li>
 *   <li>Token validation</li>
 *   <li>Password setup (activation)</li>
 *   <li>Member information update</li>
 *   <li>Member termination</li>
 *   <li>Verification of terminated state</li>
 *   <li>Second termination rejection</li>
 * </ul>
 * <p>
 * <b>Test Scope:</b>
 * <ul>
 *   <li>Verifies lifecycle progression (can proceed from step A to step B)</li>
 *   <li>Checks HTTP status codes (201, 200, 204, 400)</li>
 *   <li>Validates navigation via Location headers and minimal ID extraction</li>
 * </ul>
 * <p>
 * <b>What is NOT tested here:</b>
 * <ul>
 *   <li>Response JSON structure and field validation - tested in {@link com.klabis.members.infrastructure.restapi.MemberControllerApiTest}</li>
 *   <li>Domain events - tested in module integration tests</li>
 *   <li>Validation error messages - tested in controller unit tests</li>
 * </ul>
 */
@E2ETest
@ActiveProfiles("test")
@Sql(scripts = "/sql/test-member-lifecycle-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Member Lifecycle E2E Test")
class MemberLifecycleE2ETest {

    private static final LoggingEmailService LOGGING_EMAIL_SERVICE = new LoggingEmailService(
            EmailProperties.enabledEmail("noreply@klabis.zabiny.club"));

    static EmailService emailServiceStub() {
        return LOGGING_EMAIL_SERVICE;
    }

    @TestBean
    private EmailService emailServiceStub;

    /**
     * Test configuration to use synchronous task executor.
     * This ensures async event handlers execute synchronously during testing,
     * making it easier to verify event processing without timing issues.
     */
    @TestConfiguration
    static class SyncTaskExecutorConfiguration {

        @Bean
        TaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private static final Pattern LOCATION_PATTERN = Pattern.compile("http://localhost/api/members/(.*)");
    private static final String ADMIN_USERNAME = "admin";

    @Test
    @DisplayName("Complete member lifecycle: register → list → email → validate → password → update → terminate")
    @WithKlabisMockUser(memberId = "550e8400-e29b-41d4-a716-446655440000", authorities = {Authority.MEMBERS_CREATE, Authority.MEMBERS_READ, Authority.MEMBERS_UPDATE, Authority.MEMBERS_DELETE})
    void shouldCompleteFullMemberLifecycle() throws Exception {
        // ========================================================================
        // STEP 1: Register member
        // ========================================================================
        MvcResult registerResult = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content("""
                                        {
                                            "firstName": "Jan",
                                            "lastName": "Novák",
                                            "dateOfBirth": "2000-01-15",
                                            "nationality": "CZE",
                                            "gender": "MALE",
                                            "email": "jan.novak@example.com",
                                            "phone": "+420777123456",
                                            "address": {
                                                "street": "Hlavní 123",
                                                "city": "Praha",
                                                "postalCode": "11000",
                                                "country": "CZ"
                                            }
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andReturn();

        UUID memberId = extractMemberIdFromLocation(registerResult);

        // ========================================================================
        // STEP 2: Check member in GET /members list
        // ========================================================================
        mockMvc.perform(
                        get("/api/members")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList").isArray())
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList[?(@.id == '%s')]".formatted(memberId)).exists());

        // ========================================================================
        // STEP 3: Get member details
        // ========================================================================
        MvcResult memberDetailsResult = mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId.toString()))
                .andReturn();
        final CurrentUserData registeredMemberUserData = createCurrentUserDataFromMemberResponse(memberDetailsResult);

        // ========================================================================
        // STEP 4: Get email token from email (using LoggingEmailService)
        // ========================================================================
        String activationToken = Awaitility.waitAtMost(Duration.ofSeconds(4))
                .until(MemberLifecycleE2ETest::findActivationTokenInSentEmail, Optional::isPresent)
                .orElseThrow();

        // ========================================================================
        // STEP 5: Validate token via API
        // ========================================================================
        mockMvc.perform(get("/api/auth/password-setup/validate")
                        .param("token", activationToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        // ========================================================================
        // STEP 6: Setup password via API
        // ========================================================================
        String newPassword = "SecurePass123!@#";
        mockMvc.perform(post("/api/auth/password-setup/complete")
                        .contentType("application/json")
                        .content("""
                                {
                                    "token": "%s",
                                    "password": "%s",
                                    "passwordConfirmation": "%s"
                                }
                                """.formatted(activationToken, newPassword, newPassword))
                        .header("X-Forwarded-For", "192.168.1.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password set successfully. You can now log in."));

        // ========================================================================
        // STEP 8: New member can fetch data
        // ========================================================================
        mockMvc.perform(get("/api/members")
                        .contentType("application/json")
                        .with(klabisAuthentication(jwtTokenParams(registeredMemberUserData).withAuthorities(Authority.MEMBERS_READ))))
                .andExpect(status().isOk());



        // ========================================================================
        // STEP 7: Update member
        // ========================================================================
        mockMvc.perform(
                        patch("/api/members/{id}", memberId)
                                .contentType("application/json")
                                .content("""
                                        {
                                            "email": "jan.novy@example.com",
                                            "phone": "+420777999888",
                                            "address": {
                                                "street": "Nová 456",
                                                "city": "Brno",
                                                "postalCode": "60000",
                                                "country": "CZ"
                                            }
                                        }
                                        """)
                )
                .andExpect(status().isNoContent());

        // ========================================================================
        // STEP 8: Verify updated data
        // ========================================================================
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId.toString()));

        // ========================================================================
        // STEP 9: Suspend member
        // ========================================================================
        mockMvc.perform(
                        post("/api/members/{id}/suspend", memberId)
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content("""
                                        {
                                            "reason": "ODHLASKA",
                                            "note": "Member requested termination"
                                        }
                                        """)
                )
                .andExpect(status().isNoContent());

        // ========================================================================
        // STEP 10: Verify suspension state
        // ========================================================================
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId.toString()))
                .andExpect(jsonPath("$.active").value(false));

        // ========================================================================
        // STEP 11: New member cannot fetch data
        // ========================================================================
        mockMvc.perform(get("/api/members")
                        .contentType("application/json")
                        .with(klabisAuthentication(jwtTokenParams(registeredMemberUserData).withAuthorities(Authority.MEMBERS_READ))))
                .andExpect(status().isForbidden());


        // ========================================================================
        // STEP 12: Verify second suspension is rejected
        // ========================================================================
        mockMvc.perform(
                        post("/api/members/{id}/suspend", memberId)
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content("""
                                        {
                                            "reason": "ODHLASKA",
                                            "note": "Member requested termination"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("already suspended")));

    }

    private UUID extractMemberIdFromLocation(MvcResult result) {
        String locationHeader = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(locationHeader).isNotNull();

        Matcher matcher = LOCATION_PATTERN.matcher(locationHeader);
        if (matcher.matches()) {
            return UUID.fromString(matcher.group(1));
        } else {
            throw new IllegalArgumentException("Unexpected Location header format: " + locationHeader);
        }
    }

    private CurrentUserData createCurrentUserDataFromMemberResponse(MvcResult result) throws UnsupportedEncodingException {
        return createCurrentUserDataFromMemberResponse(result.getResponse().getContentAsString());
    }

    private CurrentUserData createCurrentUserDataFromMemberResponse(String memberDetailResponse) {
        try {
            Map<String, Object> attributes = new ObjectMapper().readValue(memberDetailResponse, Map.class);
            String registrationNumberValue = (String) attributes.get("registrationNumber");
            MemberId memberId = new MemberId(UUID.fromString((String) attributes.get("id")));
            return new CurrentUserData(registrationNumberValue, memberId.toUserId(), memberId);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<String> findActivationTokenInSentEmail() {
        return LOGGING_EMAIL_SERVICE.getLastEmailSent().map(email -> {
            Pattern passwordTokenUrl = Pattern.compile(
                    "token=([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");
            Matcher matcher = passwordTokenUrl.matcher(email.htmlBody());
            assertThat(matcher.find()).isTrue();
            return matcher.group(1);
        });
    }
}
