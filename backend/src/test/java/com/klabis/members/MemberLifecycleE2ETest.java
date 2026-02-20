package com.klabis.members;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.E2EIntegrationTest;
import com.klabis.members.domain.DeactivationReason;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.infrastructure.restapi.AddressRequest;
import com.klabis.members.infrastructure.restapi.RegisterMemberRequest;
import com.klabis.members.infrastructure.restapi.TerminateMembershipRequest;
import com.klabis.members.infrastructure.restapi.UpdateMemberRequest;
import com.klabis.users.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test for complete Member aggregate lifecycle.
 * <p>
 * This test verifies the entire member lifecycle from registration through termination:
 * <ul>
 *   <li>Member registration</li>
 *   <li>Member retrieval and verification</li>
 *   <li>Member information update</li>
 *   <li>Member termination</li>
 *   <li>Verification of terminated state</li>
 *   <li>Member presence in list (soft delete)</li>
 * </ul>
 * <p>
 * The test validates:
 * <ul>
 *   <li>Domain events (MemberCreatedEvent, MemberTerminatedEvent) in outbox</li>
 *   <li>Database state via MemberRepository</li>
 *   <li>API responses via MockMvc</li>
 * </ul>
 * <p>
 * <b>Note:</b> Password setup flow is tested separately in {@link com.klabis.members.PasswordSetupFlowE2ETest}
 * to keep test responsibilities focused and maintainable.
 */
@E2EIntegrationTest
@ActiveProfiles("test")
@Sql(scripts = "/sql/test-member-lifecycle-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Member Lifecycle E2E Test")
class MemberLifecycleE2ETest {

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

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    private static final Pattern LOCATION_PATTERN = Pattern.compile("http://localhost/api/members/(.*)");
    private static final String ADMIN_USERNAME = "admin";

    @Test
    @DisplayName("Complete member lifecycle: register → get → update → terminate → verify")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:CREATE", "MEMBERS:READ", "MEMBERS:UPDATE", "MEMBERS:DELETE"})
    void shouldCompleteFullMemberLifecycle(AssertablePublishedEvents events) throws Exception {
        // ========================================================================
        // STEP 1: Register member
        // ========================================================================
        AddressRequest address = new AddressRequest(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        RegisterMemberRequest registerRequest = new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.of(2000, 1, 15),
                "CZE",
                Gender.MALE,
                "jan.novak@example.com",
                "+420777123456",
                address,
                null,
                null,
                null
        );

        MvcResult registerResult = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(registerRequest))
                )
                .andExpect(status().isCreated())
                .andReturn();

        UUID memberId = extractMemberIdFromLocation(registerResult);

        // Verify MemberCreatedEvent in outbox
        assertThat(events)
                .contains(MemberCreatedEvent.class)
                .matching(event -> event.getMemberId().equals(new UserId(memberId)));

        // ========================================================================
        // STEP 2: Get member details
        // ========================================================================
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId.toString()))
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Novák"))
                .andExpect(jsonPath("$.dateOfBirth").value("2000-01-15"))
                .andExpect(jsonPath("$.nationality").value("CZE"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.email").value("jan.novak@example.com"))
                .andExpect(jsonPath("$.phone").value("+420777123456"))
                .andExpect(jsonPath("$.address.street").value("Hlavní 123"))
                .andExpect(jsonPath("$.address.city").value("Praha"))
                .andExpect(jsonPath("$.address.postalCode").value("11000"))
                .andExpect(jsonPath("$.address.country").value("CZ"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.collection.href").exists());

        // Verify database state
        Optional<Member> memberAfterRegistration = memberRepository.findById(new UserId(memberId));
        assertThat(memberAfterRegistration).isPresent();
        assertThat(memberAfterRegistration.get().isActive()).isTrue();
        assertThat(memberAfterRegistration.get().getEmail().value()).isEqualTo("jan.novak@example.com");

        // ========================================================================
        // STEP 3: Update member
        // ========================================================================
        AddressRequest updatedAddress = new AddressRequest(
                "Nová 456",
                "Brno",
                "60000",
                "CZ"
        );

        UpdateMemberRequest updateRequest = new UpdateMemberRequest(
                Optional.of("jan.novy@example.com"),
                Optional.of("+420777999888"),
                Optional.of(updatedAddress),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        mockMvc.perform(
                        patch("/api/members/{id}", memberId)
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(updateRequest))
                )
                .andExpect(status().isNoContent());

        // ========================================================================
        // STEP 4: Verify updated data
        // ========================================================================
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId.toString()))
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Novák"))
                .andExpect(jsonPath("$.email").value("jan.novy@example.com"))
                .andExpect(jsonPath("$.phone").value("+420777999888"))
                .andExpect(jsonPath("$.address.street").value("Nová 456"))
                .andExpect(jsonPath("$.address.city").value("Brno"))
                .andExpect(jsonPath("$.address.postalCode").value("60000"))
                .andExpect(jsonPath("$.active").value(true));

        // ========================================================================
        // STEP 5: Terminate member
        // ========================================================================
        TerminateMembershipRequest terminateRequest = new TerminateMembershipRequest(
                DeactivationReason.ODHLASKA,
                Optional.of("Member requested termination")
        );

        mockMvc.perform(
                        post("/api/members/{id}/terminate", memberId)
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(terminateRequest))
                )
                .andExpect(status().isNoContent());

        // Verify MemberTerminatedEvent in outbox
        assertThat(events)
                .contains(MemberTerminatedEvent.class)
                .matching(event -> event.getMemberId().equals(new UserId(memberId))
                        && event.getReason() == DeactivationReason.ODHLASKA);

        // ========================================================================
        // STEP 6: Verify termination state
        // ========================================================================
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId.toString()))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.deactivationReason").value("ODHLASKA"))
                .andExpect(jsonPath("$.deactivatedAt").exists())
                .andExpect(jsonPath("$.deactivationNote").value("Member requested termination"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.collection.href").exists());

        // Verify termination in database
        Optional<Member> terminatedMember = memberRepository.findById(new UserId(memberId));
        assertThat(terminatedMember).isPresent();
        assertThat(terminatedMember.get().isActive()).isFalse();
        assertThat(terminatedMember.get().getDeactivationReason()).isEqualTo(DeactivationReason.ODHLASKA);
        assertThat(terminatedMember.get().getDeactivationNote()).isEqualTo("Member requested termination");
        assertThat(terminatedMember.get().getDeactivatedAt()).isNotNull();

        // ========================================================================
        // STEP 7: Verify member in list (soft delete)
        // ========================================================================
        mockMvc.perform(
                        get("/api/members")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList").isArray())
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList[?(@.id == '%s')]".formatted(memberId)).exists());

        // ========================================================================
        // STEP 8: Verify second termination is rejected
        // ========================================================================
        mockMvc.perform(
                        post("/api/members/{id}/terminate", memberId)
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(terminateRequest))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("already terminated")));
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
}
