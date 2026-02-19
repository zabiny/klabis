package com.klabis.members;

import com.klabis.users.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Member update methods.
 * <p>
 * Tests the mutable update behavior of Member aggregate:
 * - updateContactInformation() - member-editable fields
 * - updateDocuments() - document updates
 * - updateMemberDetails() - admin-only fields
 * <p>
 * Note: After migrating to Memento pattern, update methods modify the Member
 * in-place (mutable pattern) instead of returning new instances.
 */
@DisplayName("Member Update Methods")
class MemberUpdateTest {

    private Member createTestMember() {
        LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);
        EmailAddress email = EmailAddress.of("jan.novak@example.com");
        PhoneNumber phone = PhoneNumber.of("+420123456789");
        Address address = Address.of("Hlavní 123", "Praha", "11000", "CZ");
        RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
        PersonalInformation personalInformation = PersonalInformation.of(
                "Jan",
                "Novák",
                dateOfBirth,
                "CZ",
                Gender.MALE
        );

        return Member.create(
                registrationNumber,
                personalInformation,
                address,
                email,
                phone,
                null // no guardian for adult
        );
    }

    @Nested
    @DisplayName("updateContactInformation() method")
    class UpdateContactInformationMethod {

        @Test
        @DisplayName("should update email when provided")
        void shouldUpdateEmailWhenProvided() {
            // Arrange
            Member member = createTestMember();
            EmailAddress newEmail = EmailAddress.of("new.email@example.com");

            // Act - modifies member in-place, returns void
            member.updateContactInformation(newEmail, null, null);

            // Assert
            MemberAssert.assertThat(member)
                    .hasEmail(EmailAddress.of("new.email@example.com"));
            assertThat(member.getPhone().value()).isEqualTo("+420123456789"); // unchanged
            assertThat(member.getAddress().street()).isEqualTo("Hlavní 123"); // unchanged
        }

        @Test
        @DisplayName("should update phone when provided")
        void shouldUpdatePhoneWhenProvided() {
            // Arrange
            Member member = createTestMember();
            PhoneNumber newPhone = PhoneNumber.of("+420987654321");

            // Act
            member.updateContactInformation(null, newPhone, null);

            // Assert
            assertThat(member.getPhone().value()).isEqualTo("+420987654321");
            assertThat(member.getEmail().value()).isEqualTo("jan.novak@example.com"); // unchanged
        }

        @Test
        @DisplayName("should update address when provided")
        void shouldUpdateAddressWhenProvided() {
            // Arrange
            Member member = createTestMember();
            Address newAddress = Address.of("Nová 456", "Brno", "60200", "CZ");

            // Act
            member.updateContactInformation(null, null, newAddress);

            // Assert
            assertThat(member.getAddress().street()).isEqualTo("Nová 456");
            assertThat(member.getAddress().city()).isEqualTo("Brno");
            assertThat(member.getEmail().value()).isEqualTo("jan.novak@example.com"); // unchanged
        }

        @Test
        @DisplayName("should update all contact fields when all provided")
        void shouldUpdateAllContactFieldsWhenAllProvided() {
            // Arrange
            Member member = createTestMember();
            EmailAddress newEmail = EmailAddress.of("new.email@example.com");
            PhoneNumber newPhone = PhoneNumber.of("+420987654321");
            Address newAddress = Address.of("Nová 456", "Brno", "60200", "CZ");

            // Act
            member.updateContactInformation(newEmail, newPhone, newAddress);

            // Assert
            assertThat(member.getEmail().value()).isEqualTo("new.email@example.com");
            assertThat(member.getPhone().value()).isEqualTo("+420987654321");
            assertThat(member.getAddress().street()).isEqualTo("Nová 456");
        }

        @Test
        @DisplayName("should modify same instance (mutable)")
        void shouldModifySameInstance() {
            // Arrange
            Member member = createTestMember();
            EmailAddress newEmail = EmailAddress.of("new.email@example.com");

            // Act
            member.updateContactInformation(newEmail, null, null);

            // Assert - same instance is modified
            assertThat(member.getEmail().value()).isEqualTo("new.email@example.com");
        }

        @Test
        @DisplayName("should preserve other fields when updating contact info")
        void shouldPreserveOtherFields() {
            // Arrange
            Member member = createTestMember();
            UserId originalId = member.getId();
            RegistrationNumber originalRegNum = member.getRegistrationNumber();
            String originalFirstName = member.getFirstName();
            String originalLastName = member.getLastName();
            LocalDate originalDob = member.getDateOfBirth();

            // Act
            member.updateContactInformation(EmailAddress.of("new@example.com"), null, null);

            // Assert
            assertThat(member.getId()).isEqualTo(originalId);
            assertThat(member.getRegistrationNumber()).isEqualTo(originalRegNum);
            assertThat(member.getFirstName()).isEqualTo(originalFirstName);
            assertThat(member.getLastName()).isEqualTo(originalLastName);
            assertThat(member.getDateOfBirth()).isEqualTo(originalDob);
        }

        @Test
        @DisplayName("should preserve existing email when null is passed")
        void shouldPreserveExistingEmailWhenNullIsPassed() {
            // Arrange
            Member member = createTestMember();

            // Act - null means "keep existing value", not "remove"
            member.updateContactInformation(null, null, null);

            // Assert - Email is preserved, not removed
            assertThat(member.getEmail()).isNotNull();
            assertThat(member.getEmail().value()).isEqualTo("jan.novak@example.com");
            assertThat(member.getPhone().value()).isEqualTo("+420123456789"); // unchanged
            assertThat(member.getAddress().street()).isEqualTo("Hlavní 123"); // unchanged
        }
    }

    @Nested
    @DisplayName("updateDocuments() method")
    class UpdateDocumentsMethod {

        @Test
        @DisplayName("should update identity card when provided")
        void shouldUpdateIdentityCardWhenProvided() {
            // Arrange
            Member member = createTestMember();
            IdentityCard identityCard = IdentityCard.of(
                    "AB123456",
                    LocalDate.now().plusYears(5)
            );

            // Act
            member.updateDocuments(identityCard, null, null);

            // Assert
            assertThat(member.getIdentityCard()).isNotNull();
            assertThat(member.getIdentityCard().cardNumber()).isEqualTo("AB123456");
        }

        @Test
        @DisplayName("should update medical course when provided")
        void shouldUpdateMedicalCourseWhenProvided() {
            // Arrange
            Member member = createTestMember();
            MedicalCourse medicalCourse = MedicalCourse.of(
                    LocalDate.of(2023, 1, 15),
                    Optional.of(LocalDate.of(2025, 1, 15))
            );

            // Act
            member.updateDocuments(null, medicalCourse, null);

            // Assert
            assertThat(member.getMedicalCourse()).isNotNull();
            assertThat(member.getMedicalCourse().completionDate())
                    .isEqualTo(LocalDate.of(2023, 1, 15));
        }

        @Test
        @DisplayName("should update trainer license when provided")
        void shouldUpdateTrainerLicenseWhenProvided() {
            // Arrange
            Member member = createTestMember();
            TrainerLicense trainerLicense = TrainerLicense.of(
                    "TRAINER001",
                    LocalDate.now().plusYears(3)
            );

            // Act
            member.updateDocuments(null, null, trainerLicense);

            // Assert
            assertThat(member.getTrainerLicense()).isNotNull();
            assertThat(member.getTrainerLicense().licenseNumber()).isEqualTo("TRAINER001");
        }

        @Test
        @DisplayName("should update all documents when all provided")
        void shouldUpdateAllDocumentsWhenAllProvided() {
            // Arrange
            Member member = createTestMember();
            IdentityCard identityCard = IdentityCard.of(
                    "AB123456",
                    LocalDate.now().plusYears(5)
            );
            MedicalCourse medicalCourse = MedicalCourse.of(
                    LocalDate.of(2023, 1, 15),
                    Optional.of(LocalDate.of(2025, 1, 15))
            );
            TrainerLicense trainerLicense = TrainerLicense.of(
                    "TRAINER001",
                    LocalDate.now().plusYears(3)
            );

            // Act
            member.updateDocuments(identityCard, medicalCourse, trainerLicense);

            // Assert
            assertThat(member.getIdentityCard()).isNotNull();
            assertThat(member.getMedicalCourse()).isNotNull();
            assertThat(member.getTrainerLicense()).isNotNull();
        }

        @Test
        @DisplayName("should modify same instance (mutable)")
        void shouldModifySameInstance() {
            // Arrange
            Member member = createTestMember();
            IdentityCard identityCard = IdentityCard.of(
                    "AB123456",
                    LocalDate.now().plusYears(5)
            );

            // Act
            member.updateDocuments(identityCard, null, null);

            // Assert - same instance is modified
            assertThat(member.getIdentityCard()).isNotNull();
        }

        @Test
        @DisplayName("should not validate existing documents when updating different document")
        void shouldNotValidateExistingDocumentsWhenUpdatingDifferentDocument() {
            // Arrange - Create member with a valid identity card
            LocalDate futureDate = LocalDate.now().plusYears(5);
            IdentityCard validIdentityCard = IdentityCard.of("VALID123", futureDate);

            Member memberWithDoc = Member.reconstruct(
                    createTestMember().getId(),
                    createTestMember().getRegistrationNumber(),
                    createTestMember().getPersonalInformation(),
                    createTestMember().getAddress(),
                    createTestMember().getEmail(),
                    createTestMember().getPhone(),
                    createTestMember().getGuardian(),
                    createTestMember().isActive(),
                    createTestMember().getChipNumber(),
                    validIdentityCard,
                    null,
                    null,
                    null,
                    null,
                    null, null, null, null, null, null, null
            );

            MedicalCourse medicalCourse = MedicalCourse.of(
                    LocalDate.of(2023, 1, 15),
                    Optional.empty()
            );

            // Act - Update a different document (medical course, not identity card)
            // This should NOT trigger validation of the existing identity card
            memberWithDoc.updateDocuments(null, medicalCourse, null);

            // Assert - Medical course is updated, identity card is unchanged
            assertThat(memberWithDoc.getMedicalCourse()).isNotNull();
            assertThat(memberWithDoc.getIdentityCard()).isNotNull();
            assertThat(memberWithDoc.getIdentityCard().cardNumber()).isEqualTo("VALID123"); // unchanged
            assertThat(memberWithDoc.getIdentityCard().validityDate()).isEqualTo(futureDate); // unchanged
        }
    }

    @Nested
    @DisplayName("updateMemberDetails() method")
    class UpdatePersonalDetailsMethod {

        @Test
        @DisplayName("should update gender when provided")
        void shouldUpdateGenderWhenProvided() {
            // Arrange
            Member member = createTestMember();

            // Act
            member.updateMemberDetails(
                    null, null, null, null, null,
                    null, null, null, null, null, Gender.FEMALE
            );

            // Assert
            assertThat(member.getGender()).isEqualTo(Gender.FEMALE);
            assertThat(member.getFirstName()).isEqualTo("Jan"); // unchanged
        }

        @Test
        @DisplayName("should update chip number when provided")
        void shouldUpdateChipNumberWhenProvided() {
            // Arrange
            Member member = createTestMember();

            // Act
            member.updateMemberDetails(
                    null, null, null, null, null,
                    "12345", null, null, null, null, null
            );

            // Assert
            assertThat(member.getChipNumber()).isEqualTo("12345");
        }

        @Test
        @DisplayName("should update driving license group when provided")
        void shouldUpdateDrivingLicenseGroupWhenProvided() {
            // Arrange
            Member member = createTestMember();

            // Act
            member.updateMemberDetails(
                    null, null, null, null, null,
                    null, DrivingLicenseGroup.A, null, null, null, null
            );

            // Assert
            assertThat(member.getDrivingLicenseGroup()).isEqualTo(DrivingLicenseGroup.A);
        }

        @Test
        @DisplayName("should update dietary restrictions when provided")
        void shouldUpdateDietaryRestrictionsWhenProvided() {
            // Arrange
            Member member = createTestMember();

            // Act
            member.updateMemberDetails(
                    null, null, null, null, null,
                    null, null, "Vegetarian, no nuts", null, null, null
            );

            // Assert
            assertThat(member.getDietaryRestrictions()).isEqualTo("Vegetarian, no nuts");
        }

        @Test
        @DisplayName("should update multiple fields when multiple provided")
        void shouldUpdateMultipleFieldsWhenMultipleProvided() {
            // Arrange
            Member member = createTestMember();

            // Act
            member.updateMemberDetails(
                    null, null, null, null, null,
                    "12345", DrivingLicenseGroup.A, "No dairy", null, null, Gender.FEMALE
            );

            // Assert
            assertThat(member.getChipNumber()).isEqualTo("12345");
            assertThat(member.getDrivingLicenseGroup()).isEqualTo(DrivingLicenseGroup.A);
            assertThat(member.getDietaryRestrictions()).isEqualTo("No dairy");
            assertThat(member.getGender()).isEqualTo(Gender.FEMALE);
        }

        @Test
        @DisplayName("should update all personal information when provided")
        void shouldUpdateAllPersonalInformationWhenProvided() {
            // Arrange
            Member member = createTestMember();
            PersonalInformation newPersonalInfo = PersonalInformation.of(
                    "Petr",
                    "Svoboda",
                    LocalDate.of(1985, 3, 20),
                    "CZ",
                    Gender.MALE
            );
            Address newAddress = Address.of("Nová 1", "Brno", "60200", "CZ");
            EmailAddress newEmail = EmailAddress.of("petr.svoboda@example.com");
            PhoneNumber newPhone = PhoneNumber.of("+420555555555");

            // Act
            member.updateMemberDetails(
                    newPersonalInfo, newAddress, newEmail, newPhone, null,
                    "999", DrivingLicenseGroup.B, "No gluten", null, null, null
            );

            // Assert
            assertThat(member.getFirstName()).isEqualTo("Petr");
            assertThat(member.getLastName()).isEqualTo("Svoboda");
            assertThat(member.getAddress().street()).isEqualTo("Nová 1");
            assertThat(member.getEmail().value()).isEqualTo("petr.svoboda@example.com");
            assertThat(member.getPhone().value()).isEqualTo("+420555555555");
            assertThat(member.getChipNumber()).isEqualTo("999");
            assertThat(member.getDrivingLicenseGroup()).isEqualTo(DrivingLicenseGroup.B);
            assertThat(member.getDietaryRestrictions()).isEqualTo("No gluten");
        }

        @Test
        @DisplayName("should preserve guardian when updating other fields")
        void shouldPreserveGuardianWhenUpdatingOtherFields() {
            // Arrange - Create member with guardian
            LocalDate minorDob = LocalDate.now().minusYears(10);
            PersonalInformation minorInfo = PersonalInformation.of(
                    "Anna",
                    "Nováková",
                    minorDob,
                    "CZ",
                    Gender.FEMALE
            );
            GuardianInformation guardian = new GuardianInformation(
                    "Jan",
                    "Novák",
                    "PARENT",
                    EmailAddress.of("jan.novak@example.com"),
                    PhoneNumber.of("+420111111111")
            );
            Member minor = Member.create(
                    new RegistrationNumber("ZBM9002"),
                    minorInfo,
                    createTestMember().getAddress(),
                    createTestMember().getEmail(),
                    createTestMember().getPhone(),
                    guardian
            );

            // Act
            minor.updateMemberDetails(
                    null, null, null, null, null,
                    "123", null, "No nuts", null, null, null
            );

            // Assert
            assertThat(minor.getGuardian()).isNotNull();
            assertThat(minor.getGuardian().getFirstName()).isEqualTo("Jan");
            assertThat(minor.getChipNumber()).isEqualTo("123");
        }

        @Test
        @DisplayName("should modify same instance (mutable)")
        void shouldModifySameInstance() {
            // Arrange
            Member member = createTestMember();

            // Act
            member.updateMemberDetails(
                    null, null, null, null, null,
                    "12345", null, null, null, null, null
            );

            // Assert - same instance is modified
            assertThat(member.getChipNumber()).isEqualTo("12345");
        }
    }
}
