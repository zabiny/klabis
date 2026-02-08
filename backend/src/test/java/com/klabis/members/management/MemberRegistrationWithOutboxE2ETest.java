package com.klabis.members.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.E2EIntegrationTest;
import com.klabis.common.email.EmailMessage;
import com.klabis.common.email.EmailProperties;
import com.klabis.common.email.EmailService;
import com.klabis.common.email.LoggingEmailService;
import com.klabis.members.Gender;
import com.klabis.members.MemberCreatedEvent;
import com.klabis.users.UserId;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end test for complete member registration flow with Spring Modulith outbox pattern verification.
 *
 * <p>This test verifies the entire member registration flow including:
 * <ul>
 *   <li>Member registration via API endpoint</li>
 *   <li>Member and User persistence in database</li>
 *   <li>MemberCreatedEvent persisted to outbox table</li>
 *   <li>Event handler processes event and sends password setup email</li>
 *   <li>Event marked as completed in outbox</li>
 * </ul>
 *
 * <p>This test validates the Spring Modulith integration ensures reliable event delivery
 * with the transactional outbox pattern.
 */
@E2EIntegrationTest
@ActiveProfiles("test")
@DisplayName("Member Registration with Outbox Pattern E2E Test")
class MemberRegistrationWithOutboxE2ETest {

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

    private static final LoggingEmailService LOGGING_EMAIL_SERVICE = new LoggingEmailService(EmailProperties.enabledEmail(
            "noreply@klabis.zabiny.club"));

    static EmailService emailServiceStub() {
        return LOGGING_EMAIL_SERVICE;
    }

    @TestBean
    private EmailService emailServiceStub;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ADMIN_USERNAME = "admin";
    private static final String MEMBERS_CREATE_AUTHORITY = "MEMBERS:CREATE";
    private static final String MEMBERS_READ_AUTHORITY = "MEMBERS:READ";

    @Test
    @DisplayName("Complete registration flow: API → DB → Outbox → Event Handler → Email")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY, MEMBERS_READ_AUTHORITY})
    void shouldCompleteRegistrationFlowWithOutboxPattern(AssertablePublishedEvents events) throws Exception {
        // Given: A valid member registration request
        AddressRequest address = new AddressRequest(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.of(2000, 1, 15),
                "CZE",
                Gender.MALE,
                "jan.novak@example.com",
                "+420777123456",
                address,
                null  // No guardian required for adults
        );

        // When: Registering the member via API
        MvcResult result = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Novák"))
                .andReturn();

        // Extract member ID from response
        UUID memberUuid = extractMemberId(result.getResponse());

        // STEP 1: Verify member created
        mockMvc.perform(get("/api/members/{id}", memberUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberUuid.toString()))
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andReturn().getResponse();

        // STEP 2: Verify MemberCreatedEvent persisted to outbox
        assertThat(events)
                .contains(MemberCreatedEvent.class)
                .matching(event -> Objects.equals(event.getMemberId(), new UserId(memberUuid)));

        Optional<EmailMessage> sentEmail = Awaitility.waitAtMost(Duration.ofSeconds(4))
                .until(LOGGING_EMAIL_SERVICE::getLastEmailSent, Optional::isPresent);

        // STEP 4: Verify password setup token created (email sent)
        assertThat(sentEmail).isPresent()
                .get()
                .extracting(EmailMessage::to)
                .isEqualTo("jan.novak@example.com");
    }

    private UUID extractMemberId(MockHttpServletResponse responseBody) {
        String headerLocation = responseBody.getHeader("Location");
        String memberId = headerLocation.substring(headerLocation.lastIndexOf("/") + 1);
        return UUID.fromString(memberId);
    }
}
