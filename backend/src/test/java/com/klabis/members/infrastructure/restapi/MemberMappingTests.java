package com.klabis.members.infrastructure.restapi;

import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of the MapStruct converters {@link MemberSummaryConverter} and {@link MemberDetailsConverter},
 * run against the generated implementations without a Spring web context.
 */
@DisplayName("Member MapStruct converters")
@ExtendWith(SpringExtension.class)
@Import({MemberSummaryConverterImpl.class, MemberDetailsConverterImpl.class})
class MemberMappingTests {

    @Autowired
    private MemberSummaryConverter summaryConverter;

    @Autowired
    private MemberDetailsConverter detailsConverter;

    @Test
    @DisplayName("should map member to summary response correctly")
    void shouldMapToSummaryResponseCorrectly() {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();

        MemberSummaryResponse dto = summaryConverter.convert(member);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(memberId);
        assertThat(dto.firstName()).isEqualTo("Jan");
        assertThat(dto.lastName()).isEqualTo("Novák");
        assertThat(dto.registrationNumber()).isEqualTo("ZBM1234");
    }

    @Test
    @DisplayName("should map member with all fields to details response")
    void shouldMapMemberWithAllFieldsToDetailsResponse() {
        UUID memberId = UUID.randomUUID();
        IdentityCard identityCard = new IdentityCard("IC123", LocalDate.of(2026, 12, 31));
        MedicalCourse medicalCourse = new MedicalCourse(
                LocalDate.of(2024, 1, 1),
                Optional.of(LocalDate.of(2026, 1, 1))
        );
        TrainerLicense trainerLicense = new TrainerLicense(TrainerLevel.T2, LocalDate.of(2026, 6, 30));
        RefereeLicense refereeLicense = new RefereeLicense(RefereeLevel.R2, LocalDate.of(2026, 9, 30));

        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withName("Jan", "Novák")
                .withIdentityCard(identityCard)
                .withMedicalCourse(medicalCourse)
                .withTrainerLicense(trainerLicense)
                .withRefereeLicense(refereeLicense)
                .withChipNumber("CHIP123")
                .withDrivingLicenseGroup(com.klabis.members.domain.DrivingLicenseGroup.B)
                .withDietaryRestrictions("No restrictions")
                .build();

        MemberDetailsResponse dto = detailsConverter.convert(member);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(memberId);
        assertThat(dto.firstName()).isEqualTo("Jan");
        assertThat(dto.lastName()).isEqualTo("Novák");
        assertThat(dto.registrationNumber()).isEqualTo("ZBM1234");
        assertThat(dto.email()).isEqualTo("jan.novak@example.com");
        assertThat(dto.phone()).isEqualTo("+420 123 456 789");
        assertThat(dto.address()).isNotNull();
        assertThat(dto.chipNumber()).isEqualTo("CHIP123");
        assertThat(dto.identityCard().cardNumber()).isEqualTo("IC123");
        assertThat(dto.identityCard().validityDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(dto.medicalCourse().completionDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(dto.medicalCourse().validityDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(dto.trainerLicense().level()).isEqualTo(TrainerLicenseDtoLevel.T2);
        assertThat(dto.trainerLicense().validityDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(dto.refereeLicense().level()).isEqualTo(RefereeLicenseDtoLevel.R2);
        assertThat(dto.refereeLicense().validityDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(dto.drivingLicenseGroup()).isEqualTo(
                com.klabis.members.infrastructure.restapi.DrivingLicenseGroup.B);
        assertThat(dto.dietaryRestrictions()).isEqualTo("No restrictions");
    }

    @Test
    @DisplayName("should map member with null optional fields")
    void shouldMapMemberWithNullOptionalFields() {
        Member member = MemberTestDataBuilder.aMemberWithId(UUID.randomUUID()).build();

        MemberDetailsResponse dto = detailsConverter.convert(member);

        assertThat(dto).isNotNull();
        assertThat(dto.chipNumber()).isNull();
        assertThat(dto.identityCard()).isNull();
        assertThat(dto.medicalCourse()).isNull();
        assertThat(dto.trainerLicense()).isNull();
        assertThat(dto.refereeLicense()).isNull();
        assertThat(dto.drivingLicenseGroup()).isNull();
        assertThat(dto.dietaryRestrictions()).isNull();
    }

    @Test
    @DisplayName("should map member with all documents")
    void shouldMapMemberWithAllDocuments() {
        IdentityCard identityCard = new IdentityCard("IC-ALL", LocalDate.of(2026, 12, 31));
        MedicalCourse medicalCourse = new MedicalCourse(
                LocalDate.of(2024, 1, 1),
                Optional.of(LocalDate.of(2026, 1, 1))
        );
        TrainerLicense trainerLicense = new TrainerLicense(TrainerLevel.T3, LocalDate.of(2026, 6, 30));

        Member member = MemberTestDataBuilder.aMemberWithId(UUID.randomUUID())
                .withIdentityCard(identityCard)
                .withMedicalCourse(medicalCourse)
                .withTrainerLicense(trainerLicense)
                .build();

        MemberDetailsResponse dto = detailsConverter.convert(member);

        assertThat(dto.identityCard()).isNotNull();
        assertThat(dto.medicalCourse()).isNotNull();
        assertThat(dto.trainerLicense().level()).isEqualTo(TrainerLicenseDtoLevel.T3);
    }

    @Test
    @DisplayName("should map medical course without validity date (indefinite)")
    void shouldMapMedicalCourseWithNullValidityDate() {
        MedicalCourse course = new MedicalCourse(LocalDate.of(2024, 6, 15), null);

        MedicalCourseDto dto = detailsConverter.medicalCourseToDto(course);

        assertThat(dto.completionDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(dto.validityDate()).isNull();
    }
}
