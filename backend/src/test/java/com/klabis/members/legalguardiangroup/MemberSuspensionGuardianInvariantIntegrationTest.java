package com.klabis.members.legalguardiangroup;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.users.UserId;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.Country;
import com.klabis.members.domain.DeactivationReason;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.Nationality;
import com.klabis.members.domain.PersonalInformation;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.members.legalguardian.application.LegalGuardianPort.GuardianInput;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableScenarios
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Suspended guardian is removed from groups with another guardian (integration)")
class MemberSuspensionGuardianInvariantIntegrationTest {

    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private ManagementPort managementPort;
    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupPort;

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Member registerAdult(String lastName) {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Jan", lastName, LocalDate.of(1985, 3, 10), Nationality.of("SK"), Gender.MALE),
                Address.of("Testovací 2", "Praha", "10000", Country.of("CZ")),
                EmailAddress.of(lastName.toLowerCase() + "@example.com"), PhoneNumber.of("+420777888999"), null, null, null, List.of(), null));
    }

    private Member registerMinor(List<GuardianInput> guardians) {
        LocalDate dateOfBirth = LocalDate.now().minusYears(10);
        String birthNumber = dateOfBirth.format(DateTimeFormatter.ofPattern("yyMMdd")) + "/1234";
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Dítě", "Svobodová" + unique(), dateOfBirth, Nationality.of("CZ"), Gender.FEMALE),
                Address.of("Dětská 1", "Brno", "60200", Country.of("CZ")),
                null, null, BirthNumber.of(birthNumber), null, null, guardians, null));
    }

    private static Member.SuspendMembership suspension() {
        return new Member.SuspendMembership(new UserId(UUID.randomUUID()), DeactivationReason.ODHLASKA, null);
    }

    @Test
    @DisplayName("suspending a guardian removes them from the group that keeps another guardian and resuming does not restore them")
    void suspensionRemovesGuardianFromGroupWithAnotherGuardian(Scenario scenario) {
        Member suspended = registerAdult("Novak" + unique());
        Member staying = registerAdult("Dvorak" + unique());
        Member child = registerMinor(List.of(
                GuardianInput.existing(suspended.getId().toUserId()),
                GuardianInput.existing(staying.getId().toUserId())));
        assertThat(legalGuardianGroupPort.guardiansOf(child.getId()))
                .containsExactlyInAnyOrder(suspended.getId().toUserId(), staying.getId().toUserId());

        scenario.stimulate(() -> managementPort.suspendMember(suspended.getId(), suspension(), Set.of()))
                .andWaitForStateChange(() -> legalGuardianGroupPort.guardiansOf(child.getId()),
                        guardians -> guardians.size() == 1)
                .andVerify(guardians -> assertThat(guardians).containsExactly(staying.getId().toUserId()));

        managementPort.resumeMember(suspended.getId(), new Member.ResumeMembership(new UserId(UUID.randomUUID())));

        assertThat(legalGuardianGroupPort.guardiansOf(child.getId())).containsExactly(staying.getId().toUserId());
    }
}
