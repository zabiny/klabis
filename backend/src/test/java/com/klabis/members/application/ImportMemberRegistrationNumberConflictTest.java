package com.klabis.members.application;

import com.klabis.TestApplicationConfiguration;
import com.klabis.members.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the spec scenario "A registration number already in use is refused" (see
 * openspec/changes/import-oris-members/specs/members/spec.md).
 * <p>
 * The `members.registration_number` UNIQUE constraint is what enforces this - no extra
 * existence check is added in the service, since that would just be a race (see design.md D4).
 * This test proves the DB-level violation surfaces as a domain-meaningful exception rather than
 * a raw {@link org.springframework.dao.DataIntegrityViolationException}.
 * <p>
 * Uses {@code @SpringBootTest} rather than {@code @ApplicationModuleTest} - the latter runs
 * Spring Modulith's module-boundary verification, which currently fails on a pre-existing
 * events/finance/members/oris slice cycle unrelated to this change (also reproduces on the
 * existing {@code RegisterMemberAutoProvisioningTest}).
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Importing a member with an already-used registration number")
@Import(TestApplicationConfiguration.class)
class ImportMemberRegistrationNumberConflictTest {

    @Autowired
    private RegistrationPort registrationPort;

    @Autowired
    private MemberRepository memberRepository;

    private RegistrationPort.RegisterNewMember detailsFor(String email, LocalDate dateOfBirth) {
        return new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Jan", "Novák", dateOfBirth, "CZ", Gender.MALE),
                Address.of("Testovací 1", "Praha", "10000", "CZ"),
                EmailAddress.of(email),
                PhoneNumber.of("+420777888999"),
                BirthNumber.of(dateOfBirth.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd")) + "/1234"),
                null,
                null
        );
    }

    @Test
    @DisplayName("is refused and creates no member")
    void refusesDuplicateRegistrationNumberAndCreatesNoMember() {
        RegistrationNumber registrationNumber = RegistrationNumber.of("ZBM0099");

        RegistrationPort.RegisterNewMember firstDetails = detailsFor("first-import@example.com", LocalDate.of(2000, 1, 15));
        registrationPort.importMember(new RegistrationPort.ImportMember(firstDetails, registrationNumber));

        RegistrationPort.RegisterNewMember secondDetails = detailsFor("second-import@example.com", LocalDate.of(2000, 2, 20));
        RegistrationPort.ImportMember duplicateCommand = new RegistrationPort.ImportMember(secondDetails, registrationNumber);

        assertThatThrownBy(() -> registrationPort.importMember(duplicateCommand))
                .isNotInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .isInstanceOf(com.klabis.common.exceptions.BusinessRuleViolationException.class);

        assertThat(memberRepository.findByEmail("second-import@example.com")).isEmpty();
    }
}
