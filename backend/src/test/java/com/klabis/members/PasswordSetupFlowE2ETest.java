package com.klabis.members;

import com.klabis.E2EIntegrationTest;
import com.klabis.common.email.EmailProperties;
import com.klabis.common.email.EmailService;
import com.klabis.common.email.LoggingEmailService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.Scenario;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test for the complete password setup flow.
 *
 * <p>This test verifies the entire user journey from registration to successful login:
 * register member → email sent (token generated) → validate token → setup password → login
 *
 * <p>Uses TestContainers for real PostgreSQL database and MockMvc for API testing.
 */
@E2EIntegrationTest
@ActiveProfiles("test")
@DisplayName("Password Setup Flow E2E Integration Test")
class PasswordSetupFlowE2ETest {

    private static final LoggingEmailService LOGGING_EMAIL_SERVICE = new LoggingEmailService(EmailProperties.enabledEmail(
            "noreply@klabis.zabiny.club"));

    static EmailService emailServiceStub() {
        return LOGGING_EMAIL_SERVICE;
    }

    @TestBean
    private EmailService emailServiceStub;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Happy path: register → validate token → setup password → verify user can login")
    @WithMockUser(authorities = "MEMBERS:CREATE")
    void shouldCompletePasswordSetupFlowSuccessfully(Scenario scenario) throws Exception {
        // STEP 1: Register a new member
        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content("""
                  {
                    "firstName": "John",
                    "lastName": "Doe",
                    "dateOfBirth": "1990-01-15",
                    "nationality": "CZ",
                    "gender": "MALE",
                    "email": "john.doe@example.com",
                    "phone": "+420123456789",
                    "address": {
                      "street": "Street 1",
                      "city": "City",
                      "postalCode": "10000",
                      "country": "CZ"
                    }
                  }
                """)).andExpect(status().isCreated());


        // STEP 2: Get the password setup token (simulates retrieving from email).
        String activationToken = Awaitility.waitAtMost(Duration.ofSeconds(4))
                .until(PasswordSetupFlowE2ETest::findActivationTokenInSentEmail, Optional::isPresent)
                .orElseThrow();

        // STEP 3: Validate the token via API
        mockMvc.perform(get("/api/auth/password-setup/validate")
                        .param("token", activationToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        // STEP 4: Complete password setup via API
        String newPassword = "SecurePass123!@#";
        mockMvc.perform(post("/api/auth/password-setup/complete")
                        .contentType(MediaType.APPLICATION_JSON)
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
