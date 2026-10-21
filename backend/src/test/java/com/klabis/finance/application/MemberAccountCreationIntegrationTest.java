package com.klabis.finance.application;

import com.klabis.TestApplicationConfiguration;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.common.users.UserId;
import com.klabis.finance.domain.Money;
import com.klabis.members.MemberId;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@EnableScenarios
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@DisplayName("MemberAccount creation integration test")
class MemberAccountCreationIntegrationTest {

    @Autowired
    private RegistrationPort registrationPort;

    @Autowired
    private MemberAccountRepository memberAccountRepository;

    @Autowired
    private ManagementPort managementPort;

    @Autowired
    private DepositPort depositPort;

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
                null
        );

        var memberRef = new AtomicReference<Member>();

        scenario.stimulate(() -> memberRef.set(registrationPort.registerMember(command)))
                .andWaitForStateChange(() -> memberAccountRepository.findById(memberRef.get().getId()))
                .andVerify(account -> {
                    assertThat(account)
                            .as("MemberAccount should be created for member %s", memberRef.get().getId())
                            .isPresent();
                    assertThat(account.get().getBalance().isZero())
                            .as("Initial balance should be zero")
                            .isTrue();
                });
    }

    @Test
    @DisplayName("account stays available and accepts transactions after the membership is suspended")
    void accountSurvivesMembershipSuspension(Scenario scenario) throws Exception {
        RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Petr", "Svoboda", LocalDate.of(1985, 3, 10), "SK", Gender.MALE),
                Address.of("Testovací 2", "Praha", "10000", "CZ"),
                EmailAddress.of("petr.svoboda.finance.test@example.com"),
                PhoneNumber.of("+420777888111"),
                null,
                null,
                null
        );
        var memberRef = new AtomicReference<Member>();

        scenario.stimulate(() -> memberRef.set(registrationPort.registerMember(command)))
                .andWaitForStateChange(() -> memberAccountRepository.findById(memberRef.get().getId()))
                .andVerify(account -> assertThat(account).isPresent());

        MemberId memberId = memberRef.get().getId();
        UserId manager = new UserId(UUID.randomUUID());
        managementPort.suspendMember(memberId,
                new Member.SuspendMembership(manager, DeactivationReason.ODHLASKA, null));

        depositPort.deposit(new DepositPort.DepositCommand(
                memberId, new BigDecimal("100"), LocalDate.now(), "after suspension", manager));

        var account = memberAccountRepository.findById(memberId);
        assertThat(account).isPresent();
        assertThat(account.get().getBalance()).isEqualTo(Money.ofCzk(new BigDecimal("100")));
    }
}
