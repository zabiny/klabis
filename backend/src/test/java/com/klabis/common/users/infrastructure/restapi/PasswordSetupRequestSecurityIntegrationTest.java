package com.klabis.common.users.infrastructure.restapi;

import com.klabis.CleanupTestData;
import com.klabis.KlabisModuleTest;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.email.LoggingEmailService;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.PersonalInformation;
import com.klabis.members.domain.PhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of design.md D1: a foreign e-mail must get exactly the same response
 * as a matching one, and must trigger no e-mail (task 1.4).
 */
@KlabisModuleTest(extraIncludes = {"members", "sync"})
@AutoConfigureMockMvc
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("POST /api/auth/password-setup/request — activation contact verification")
class PasswordSetupRequestSecurityIntegrationTest {

    private static final String OWN_EMAIL = "member-8001@example.com";
    private static final String FOREIGN_EMAIL = "attacker@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegistrationPort registrationPort;

    @Autowired
    private LoggingEmailService loggingEmailService;

    private String registrationNumber;

    @BeforeEach
    void registerPendingMember() {
        Member member = registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Jan", "Novák", LocalDate.of(1990, 1, 1), "SK", Gender.MALE),
                Address.of("Hlavní 1", "Praha", "110 00", "CZ"),
                EmailAddress.of(OWN_EMAIL),
                PhoneNumber.of("+420123456789"),
                null,
                null,
                null
        ));
        registrationNumber = member.getRegistrationNumber().getValue();
    }

    @Test
    @DisplayName("a foreign e-mail gets the same response and sends no mail")
    void foreignEmailGetsSameResponseAndSendsNoMail() throws Exception {
        // The logging spy keeps only the last message across the whole Spring context, so a
        // marker distinguishes "nothing new was sent" from "something was sent earlier".
        var beforeRequest = loggingEmailService.getLastEmailSent();

        String requestBody = objectMapper.writeValueAsString(
                TokenRequestRequestBuilder.builder()
                        .registrationNumber(registrationNumber)
                        .email(FOREIGN_EMAIL)
                        .build());

        mockMvc.perform(post("/api/auth/password-setup/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If your account is pending activation, you will receive an email with a new setup link."));

        assertThat(loggingEmailService.getLastEmailSent()).isEqualTo(beforeRequest);
    }

    @Test
    @DisplayName("a matching e-mail gets the same response body and sends the link")
    void matchingEmailGetsSameResponseAndSendsMail() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                TokenRequestRequestBuilder.builder()
                        .registrationNumber(registrationNumber)
                        .email(OWN_EMAIL)
                        .build());

        mockMvc.perform(post("/api/auth/password-setup/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If your account is pending activation, you will receive an email with a new setup link."));

        assertThat(loggingEmailService.getLastEmailSent()).isPresent();
        assertThat(loggingEmailService.getLastEmailSent().get().to()).isEqualTo(OWN_EMAIL);
    }
}
