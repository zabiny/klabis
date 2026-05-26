package com.klabis.finance.application;

import com.klabis.TestApplicationConfiguration;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.members.MemberId;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableScenarios
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@DisplayName("MemberAccount creation integration test")
class MemberAccountCreationIntegrationTest {

    @Autowired
    private RegistrationPort registrationPort;

    @Autowired
    private MemberAccountRepository memberAccountRepository;

    @Test
    @DisplayName("registering a member results in a finance account being created")
    void registeringMemberResultsInFinanceAccountBeingCreated(Scenario scenario) throws Exception {
        RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Jan", "Novák", LocalDate.of(1990, 5, 15), "SK", Gender.MALE),
                Address.of("Testovací 1", "Praha", "10000", "CZ"),
                EmailAddress.of("jan.novak.finance.test@example.com"),
                PhoneNumber.of("+420777888999"),
                null,
                null,
                null,
                null
        );

        scenario.stimulate(() -> registrationPort.registerMember(command))
                .andWaitForEventOfType(com.klabis.members.MemberCreatedEvent.class)
                .toArriveAndVerify(event -> {
                    MemberId memberId = event.memberId();
                    assertThat(memberAccountRepository.findById(memberId))
                            .as("MemberAccount should be created for member %s", memberId)
                            .isPresent();
                    assertThat(memberAccountRepository.findById(memberId).get().getBalance().isZero())
                            .as("Initial balance should be zero")
                            .isTrue();
                });
    }
}
