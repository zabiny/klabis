package com.klabis.members.application;

import com.klabis.TestApplicationConfiguration;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserRepository;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import com.klabis.members.MemberId;
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
import java.time.Period;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the spec scenario "Registering brings its usual consequences" (see
 * openspec/changes/import-oris-members/specs/member-synchronization/spec.md): a member brought
 * in via {@link RegistrationPort#importMember} gets a {@link User}, a financial account and
 * age-based training-group assignment identically to a hand-registered member.
 * <p>
 * This is provable by construction per design.md D4 - {@code importMember} converges onto the
 * same {@code register(...)} path as {@code registerMember} immediately after the registration
 * number is decided, so every listener that reacts to {@code MemberCreatedEvent} fires exactly
 * as it would for a hand registration. This test confirms those listeners actually fire end to
 * end, the same way {@code MemberAccountCreationIntegrationTest} does for hand registration.
 */
@SpringBootTest
@EnableScenarios
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@DisplayName("Importing a member brings its usual consequences")
class ImportMemberUsualConsequencesTest {

    @Autowired
    private RegistrationPort registrationPort;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberAccountRepository memberAccountRepository;

    @Autowired
    private TrainingGroupRepository trainingGroupRepository;

    @Test
    @DisplayName("creates a User, a financial account and assigns an age-matching training group")
    void importedMemberGetsUsualRegistrationConsequences(Scenario scenario) throws Exception {
        LocalDate dateOfBirth = LocalDate.now().minus(Period.ofYears(12));
        TrainingGroup group = TrainingGroup.create(new TrainingGroup.CreateTrainingGroup(
                "Import Consequences Group",
                new MemberId(java.util.UUID.randomUUID()),
                new AgeRange(6, 15)
        ));
        trainingGroupRepository.save(group);

        GuardianInformation guardian = new GuardianInformation(
                "Parent", "Guardian", "PARENT",
                EmailAddress.of("guardian.consequences.test@example.com"),
                PhoneNumber.of("+420777654321")
        );

        RegistrationPort.RegisterNewMember details = new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Imported", "Child", dateOfBirth, "CZ", Gender.MALE),
                Address.of("Importovaná 1", "Praha", "10000", "CZ"),
                EmailAddress.of("imported.consequences.test@example.com"),
                PhoneNumber.of("+420777123456"),
                guardian,
                BirthNumber.of(dateOfBirth.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd")) + "/1234"),
                null,
                null
        );
        RegistrationPort.ImportMember command = new RegistrationPort.ImportMember(details, RegistrationNumber.of("ZBM0098"));

        var memberRef = new AtomicReference<Member>();

        scenario.stimulate(() -> memberRef.set(registrationPort.importMember(command)))
                .andWaitForStateChange(() -> memberAccountRepository.findById(memberRef.get().getId()))
                .andVerify(account -> {
                    Member member = memberRef.get();

                    assertThat(account)
                            .as("MemberAccount should be created for imported member %s", member.getId())
                            .isPresent();
                    assertThat(account.get().getBalance().isZero())
                            .as("Initial balance should be zero")
                            .isTrue();

                    assertThat(userRepository.findByUsername("ZBM0098"))
                            .as("User should be created with the imported registration number as username")
                            .isPresent();

                    TrainingGroup reloadedGroup = trainingGroupRepository.findById(group.getId()).orElseThrow();
                    assertThat(reloadedGroup.getMembers())
                            .as("Imported member should be auto-assigned to the age-matching training group")
                            .extracting(membership -> membership.memberId())
                            .contains(member.getId());
                });
    }
}
