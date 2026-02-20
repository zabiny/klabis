package com.klabis.members.infrastructure.restapi;

import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.*;
import com.klabis.members.management.IdentityCardDto;
import com.klabis.members.management.MedicalCourseDto;
import com.klabis.members.management.TrainerLicenseDto;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * Unit tests for MemberController DTO mapping methods.
 * <p>
 * Tests direct mapping functionality for all 5 mapping methods to ensure
 * correct field-by-field transformation and null handling before migrating to MapStruct.
 * <p>
 * Uses reflection to access private mapping methods for testing purposes.
 */
@DisplayName("Member Controller Mapping Tests")
@ExtendWith(SpringExtension.class)
@Import(MemberMapperImpl.class)
class MemberMappingTests {

    @Autowired
    private MemberMapper testedSubject;

    @Nested
    @DisplayName("mapToIdentityCardDto()")
    class IdentityCardDtoMappingTests {

        @Test
        @DisplayName("should map valid identity card correctly")
        void shouldMapValidIdentityCardCorrectly() {
            IdentityCard card = new IdentityCard("ABC123", LocalDate.of(2026, 12, 31));
            IdentityCardDto dto = testedSubject.identityCardToDto(card);

            assertThat(dto).isNotNull();
            assertThat(dto.cardNumber()).isEqualTo("ABC123");
            assertThat(dto.validityDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        }

        @Test
        @DisplayName("should return null when identity card is null")
        void shouldReturnNullWhenIdentityCardIsNull() {
            IdentityCardDto dto = testedSubject.identityCardToDto(null);
            assertThat(dto).isNull();
        }

        // TODO: this needs to be corrected - value object must be possible to create with date in past. That validation must be in places where editation happens (validity date must not be in past when value is edited)
        @Disabled
        @Test
        @DisplayName("should map identity card with past validity date")
        void shouldMapIdentityCardWithPastValidityDate() {
            IdentityCard card = new IdentityCard("XYZ789", LocalDate.of(2020, 1, 1));
            IdentityCardDto dto = testedSubject.identityCardToDto(card);

            assertThat(dto.validityDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        }

        @Test
        @DisplayName("should map identity card with today as validity date")
        void shouldMapIdentityCardWithTodayValidityDate() {
            LocalDate today = LocalDate.now();
            IdentityCard card = new IdentityCard("TODAY01", today);
            IdentityCardDto dto = testedSubject.identityCardToDto(card);

            assertThat(dto.validityDate()).isEqualTo(today);
        }
    }

    @Nested
    @DisplayName("mapToMedicalCourseDto()")
    class MedicalCourseDtoMappingTests {

        @Test
        @DisplayName("should map valid medical course correctly")
        void shouldMapValidMedicalCourseCorrectly() {
            MedicalCourse course = new MedicalCourse(
                    LocalDate.of(2024, 6, 15),
                    Optional.of(LocalDate.of(2026, 6, 15))
            );
            MedicalCourseDto dto = testedSubject.medicalCourseToDto(course);


            assertThat(dto).isNotNull();
            assertThat(dto.completionDate()).isEqualTo(LocalDate.of(2024, 6, 15));
            assertThat(dto.validityDate()).hasValue(LocalDate.of(2026, 6, 15));
        }

        @Test
        @DisplayName("should return null when medical course is null")
        void shouldReturnNullWhenMedicalCourseIsNull() {
            MedicalCourseDto dto = testedSubject.medicalCourseToDto(null);

            assertThat(dto).isNull();
        }

        @Test
        @DisplayName("should map medical course with null validity date (indefinite)")
        void shouldMapMedicalCourseWithNullValidityDate() {
            MedicalCourse course = new MedicalCourse(LocalDate.of(2024, 6, 15), null);
            MedicalCourseDto dto = testedSubject.medicalCourseToDto(course);

            assertThat(dto.completionDate()).isEqualTo(LocalDate.of(2024, 6, 15));
            assertThat(dto.validityDate()).isEmpty();
        }

        @Test
        @DisplayName("should map medical course with past completion date")
        void shouldMapMedicalCourseWithPastCompletionDate() {
            MedicalCourse course = new MedicalCourse(
                    LocalDate.of(2020, 1, 1),
                    Optional.of(LocalDate.of(2025, 1, 1))
            );
            MedicalCourseDto dto = testedSubject.medicalCourseToDto(course);

            assertThat(dto.completionDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        }
    }

    @Nested
    @DisplayName("mapToTrainerLicenseDto()")
    class TrainerLicenseDtoMappingTests {

        @Test
        @DisplayName("should map valid trainer license correctly")
        void shouldMapValidTrainerLicenseCorrectly() {
            TrainerLicense license = new TrainerLicense("TRN-456", LocalDate.of(2026, 12, 31));
            TrainerLicenseDto dto = testedSubject.trainerLicenseToDto(license);

            assertThat(dto).isNotNull();
            assertThat(dto.licenseNumber()).isEqualTo("TRN-456");
            assertThat(dto.validityDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        }

        @Test
        @DisplayName("should return null when trainer license is null")
        void shouldReturnNullWhenTrainerLicenseIsNull() {
            TrainerLicenseDto dto = testedSubject.trainerLicenseToDto(null);
            assertThat(dto).isNull();
        }

        // TODO: this needs to be corrected - value object must be possible to create with date in past. That validation must be in places where editation happens (validity date must not be in past when value is edited)
        @Disabled
        @Test
        @DisplayName("should map trainer license with past validity date")
        void shouldMapTrainerLicenseWithPastValidityDate() {
            TrainerLicense license = new TrainerLicense("OLD-001", LocalDate.of(2020, 1, 1));
            TrainerLicenseDto dto = testedSubject.trainerLicenseToDto(license);

            assertThat(dto.licenseNumber()).isEqualTo("OLD-001");
            assertThat(dto.validityDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        }

        @Test
        @DisplayName("should map trainer license with today as validity date")
        void shouldMapTrainerLicenseWithTodayValidityDate() {
            LocalDate today = LocalDate.now();
            TrainerLicense license = new TrainerLicense("TODAY-02", today);
            TrainerLicenseDto dto = testedSubject.trainerLicenseToDto(license);

            assertThat(dto.validityDate()).isEqualTo(today);
        }
    }

    @Nested
    @DisplayName("toSummaryResponse()")
    class MemberSummaryResponseMappingTests {

        @Test
        @DisplayName("should map member to summary response correctly")
        void shouldMapToSummaryResponseCorrectly() {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();

            MemberSummaryResponse dto = testedSubject.toSummaryResponse(member);

            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(memberId);
            assertThat(dto.firstName()).isEqualTo("Jan");
            assertThat(dto.lastName()).isEqualTo("Novák");
            assertThat(dto.registrationNumber()).isEqualTo("ZBM1234");
        }

        @Test
        @DisplayName("should map member with different name")
        void shouldMapMemberWithDifferentName() {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withName("Petra", "Nováková")
                    .build();

            MemberSummaryResponse dto = testedSubject.toSummaryResponse(member);

            assertThat(dto.firstName()).isEqualTo("Petra");
            assertThat(dto.lastName()).isEqualTo("Nováková");
        }

        @Test
        @DisplayName("should map member with different registration number")
        void shouldMapMemberWithDifferentRegistrationNumber() {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withRegistrationNumber("ZBM9999")
                    .build();

            MemberSummaryResponse dto = testedSubject.toSummaryResponse(member);

            assertThat(dto.registrationNumber()).isEqualTo("ZBM9999");
        }
    }

    @Nested
    @DisplayName("mapToDetailsResponse()")
    class MemberDetailsResponseMappingTests {

        @Test
        @DisplayName("should map member with all fields to details response")
        void shouldMapMemberWithAllFieldsToDetailsResponse() {
            UUID memberId = UUID.randomUUID();
            IdentityCard identityCard = new IdentityCard("IC123", LocalDate.of(2026, 12, 31));
            MedicalCourse medicalCourse = new MedicalCourse(
                    LocalDate.of(2024, 1, 1),
                    Optional.of(LocalDate.of(2026, 1, 1))
            );
            TrainerLicense trainerLicense = new TrainerLicense("TL456", LocalDate.of(2026, 6, 30));

            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withName("Jan", "Novák")
                    .withIdentityCard(identityCard)
                    .withMedicalCourse(medicalCourse)
                    .withTrainerLicense(trainerLicense)
                    .withChipNumber("CHIP123")
                    .withDrivingLicenseGroup(DrivingLicenseGroup.B)
                    .withDietaryRestrictions("No restrictions")
                    .build();

            MemberDetailsResponse dto = testedSubject.toDetailsResponse(member);

            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(memberId);
            assertThat(dto.firstName()).isEqualTo("Jan");
            assertThat(dto.lastName()).isEqualTo("Novák");
            assertThat(dto.registrationNumber()).isEqualTo("ZBM1234");
            assertThat(dto.email()).isEqualTo("jan.novak@example.com");
            assertThat(dto.phone()).isEqualTo("+420 123 456 789");
            assertThat(dto.address()).isNotNull();
            assertThat(dto.guardian()).isNotNull();
            assertThat(dto.chipNumber()).isEqualTo("CHIP123");
            assertThat(dto.identityCard()).isNotNull();
            assertThat(dto.identityCard().cardNumber()).isEqualTo("IC123");
            assertThat(dto.medicalCourse()).isNotNull();
            assertThat(dto.medicalCourse().completionDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(dto.trainerLicense()).isNotNull();
            assertThat(dto.trainerLicense().licenseNumber()).isEqualTo("TL456");
            assertThat(dto.drivingLicenseGroup()).isEqualTo(DrivingLicenseGroup.B);
            assertThat(dto.dietaryRestrictions()).isEqualTo("No restrictions");
        }

        @Test
        @DisplayName("should map member with null optional fields")
        void shouldMapMemberWithNullOptionalFields() {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();

            MemberDetailsResponse dto = testedSubject.toDetailsResponse(member);

            assertThat(dto).isNotNull();
            assertThat(dto.chipNumber()).isNull();
            assertThat(dto.identityCard()).isNull();
            assertThat(dto.medicalCourse()).isNull();
            assertThat(dto.trainerLicense()).isNull();
            assertThat(dto.drivingLicenseGroup()).isNull();
            assertThat(dto.dietaryRestrictions()).isNull();
        }

        @Test
        @DisplayName("should map member with only identity card")
        void shouldMapMemberWithOnlyIdentityCard() {
            UUID memberId = UUID.randomUUID();
            IdentityCard identityCard = new IdentityCard("ONLY-IC", LocalDate.of(2026, 12, 31));

            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withIdentityCard(identityCard)
                    .build();

            MemberDetailsResponse dto = testedSubject.toDetailsResponse(member);

            assertThat(dto.identityCard()).isNotNull();
            assertThat(dto.identityCard().cardNumber()).isEqualTo("ONLY-IC");
            assertThat(dto.medicalCourse()).isNull();
            assertThat(dto.trainerLicense()).isNull();
        }

        @Test
        @DisplayName("should map member with only medical course")
        void shouldMapMemberWithOnlyMedicalCourse() {
            UUID memberId = UUID.randomUUID();
            MedicalCourse medicalCourse = new MedicalCourse(
                    LocalDate.of(2024, 6, 15),
                    Optional.of(LocalDate.of(2026, 6, 15))
            );

            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withMedicalCourse(medicalCourse)
                    .build();

            MemberDetailsResponse dto = testedSubject.toDetailsResponse(member);

            assertThat(dto.identityCard()).isNull();
            assertThat(dto.medicalCourse()).isNotNull();
            assertThat(dto.medicalCourse().completionDate()).isEqualTo(LocalDate.of(2024, 6, 15));
            assertThat(dto.trainerLicense()).isNull();
        }

        @Test
        @DisplayName("should map member with only trainer license")
        void shouldMapMemberWithOnlyTrainerLicense() {
            UUID memberId = UUID.randomUUID();
            TrainerLicense trainerLicense = new TrainerLicense("ONLY-TL", LocalDate.of(2026, 12, 31));

            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withTrainerLicense(trainerLicense)
                    .build();

            MemberDetailsResponse dto = testedSubject.toDetailsResponse(member);

            assertThat(dto.identityCard()).isNull();
            assertThat(dto.medicalCourse()).isNull();
            assertThat(dto.trainerLicense()).isNotNull();
            assertThat(dto.trainerLicense().licenseNumber()).isEqualTo("ONLY-TL");
        }

        @Test
        @DisplayName("should map member with all documents")
        void shouldMapMemberWithAllDocuments() {
            UUID memberId = UUID.randomUUID();
            IdentityCard identityCard = new IdentityCard("IC-ALL", LocalDate.of(2026, 12, 31));
            MedicalCourse medicalCourse = new MedicalCourse(
                    LocalDate.of(2024, 1, 1),
                    Optional.of(LocalDate.of(2026, 1, 1))
            );
            TrainerLicense trainerLicense = new TrainerLicense("TL-ALL", LocalDate.of(2026, 6, 30));

            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withIdentityCard(identityCard)
                    .withMedicalCourse(medicalCourse)
                    .withTrainerLicense(trainerLicense)
                    .build();

            MemberDetailsResponse dto = testedSubject.toDetailsResponse(member);

            assertThat(dto.identityCard()).isNotNull();
            assertThat(dto.medicalCourse()).isNotNull();
            assertThat(dto.trainerLicense()).isNotNull();
        }
    }
}
