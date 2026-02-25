package com.klabis.members.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;
import com.klabis.members.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static com.klabis.members.MemberTestDataBuilder.aMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD Test for Member aggregate root.
 * <p>
 * Tests business rules and invariants:
 * - Member creation with valid data
 * - Registration number uniqueness
 * - Rodne cislo only for Czech nationality
 * - At least one contact (email + phone) required
 * - Guardian required for minors (<18 years)
 * - Member update methods (contact information, documents, personal details)
 * - Member termination
 */
@DisplayName("Member Aggregate")
class MemberTest {

    @Nested
    @DisplayName("RegisterMember command tests")
    class RegisterMemberCommandTests {

        @Test
        @DisplayName("should create adult member with valid data")
        void shouldCreateAdultMemberWithValidData() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);
            EmailAddress email = new EmailAddress("jan.novak@example.com");
            PhoneNumber phone = new PhoneNumber("+420123456789");
            Address address = new Address("Hlavní 123", "Praha", "11000", "CZ");
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE
            );
            UserId memberId = new UserId(UUID.randomUUID());

            // Act
            Member.RegisterMember command = new Member.RegisterMember(
                    memberId,
                    registrationNumber,
                    personalInformation,
                    address,
                    email,
                    phone,
                    null, // no guardian for adult
                    null, // no birthNumber
                    null  // no bankAccountNumber
            );
            Member member = Member.register(command);

            // Assert
            MemberAssert.assertThat(member)
                    .hasRegistrationNumber(registrationNumber)
                    .hasFirstName("Jan")
                    .hasLastName("Novák")
                    .hasDateOfBirth(dateOfBirth)
                    .hasNationality("CZ")
                    .hasGender(Gender.MALE)
                    .isActive()
                    .hasEmail(email)
                    .hasPhone(phone)
                    .hasAddress(address);

            assertThat(member.getDomainEvents())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(MemberCreatedEvent.class);

            MemberCreatedEvent event = (MemberCreatedEvent) member.getDomainEvents().get(0);
            assertThat(event.getEventId()).isNotNull();
            assertThat(event.getMemberId()).isEqualTo(member.getId());
            assertThat(event.getRegistrationNumber()).isEqualTo(registrationNumber);
            assertThat(event.getFirstName()).isEqualTo("Jan");
            assertThat(event.getLastName()).isEqualTo("Novák");
            assertThat(event.getDateOfBirth()).isEqualTo(dateOfBirth);
            assertThat(event.getNationality()).isEqualTo("CZ");
            assertThat(event.getGender()).isEqualTo(Gender.MALE);
            assertThat(event.getAddress()).isEqualTo(address);
            assertThat(event.getEmail()).isPresent().contains(email);
            assertThat(event.getPhone()).isPresent().contains(phone);
            assertThat(event.getGuardian()).isNull();
            assertThat(event.isMinor()).isFalse();
            assertThat(event.getPrimaryEmail()).isEqualTo("jan.novak@example.com");
            assertThat(event.getOccurredAt()).isNotNull();

        }

        @Test
        @DisplayName("should create minor with guardian")
        void shouldCreateMinorWithGuardian() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.now().minusYears(10); // 10 years old
            int birthYear = dateOfBirth.getYear() % 100;
            RegistrationNumber registrationNumber = new RegistrationNumber(
                    String.format("ZBM%02d01", birthYear)
            );
            Address address = new Address("Školská 456", "Brno", "60200", "CZ");
            GuardianInformation guardian = new GuardianInformation(
                    "Petr",
                    "Novák",
                    "PARENT",
                    EmailAddress.of("petr.novak@example.com"),
                    PhoneNumber.of("+420987654321")
            );
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Anna",
                    "Nováková",
                    dateOfBirth,
                    "CZ",
                    Gender.FEMALE
            );
            UserId memberId = new UserId(UUID.randomUUID());

            // Act
            Member.RegisterMember command = new Member.RegisterMember(
                    memberId,
                    registrationNumber,
                    personalInformation,
                    address,
                    null, // no member email
                    null, // no member phone
                    guardian,
                    null,
                    null
            );
            Member member = Member.register(command);

            // Assert
            MemberAssert.assertThat(member)
                    .hasRegistrationNumber(registrationNumber)
                    .hasGuardianNotNull();
            assertThat(member.getGuardian().getFirstName()).isEqualTo("Petr");

            assertThat(member.getDomainEvents())
                    .hasSize(1)
                    .first()
                    .isInstanceOf(MemberCreatedEvent.class);

            MemberCreatedEvent event = (MemberCreatedEvent) member.getDomainEvents().get(0);
            assertThat(event.getEventId()).isNotNull();
            assertThat(event.getMemberId()).isEqualTo(member.getId());
            assertThat(event.getRegistrationNumber()).isEqualTo(registrationNumber);
            assertThat(event.getFirstName()).isEqualTo("Anna");
            assertThat(event.getLastName()).isEqualTo("Nováková");
            assertThat(event.getDateOfBirth()).isEqualTo(dateOfBirth);
            assertThat(event.getNationality()).isEqualTo("CZ");
            assertThat(event.getGender()).isEqualTo(Gender.FEMALE);
            assertThat(event.getAddress()).isEqualTo(address);
            assertThat(event.getEmail()).isEmpty();
            assertThat(event.getPhone()).isEmpty();
            assertThat(event.getGuardian()).isNotNull();
            assertThat(event.getGuardian().getFirstName()).isEqualTo("Petr");
            assertThat(event.getGuardian().getLastName()).isEqualTo("Novák");
            assertThat(event.getGuardian().getRelationship()).isEqualTo("PARENT");
            assertThat(event.getGuardian().getEmail()).isEqualTo(EmailAddress.of("petr.novak@example.com"));
            assertThat(event.getGuardian().getPhone()).isEqualTo(PhoneNumber.of("+420987654321"));
            assertThat(event.isMinor()).isTrue();
            assertThat(event.getPrimaryEmail()).isEqualTo("petr.novak@example.com");
            assertThat(event.getOccurredAt()).isNotNull();

        }

        @Test
        @DisplayName("should use guardian email as primary when member has no email in MemberCreatedEvent")
        void shouldUseGuardianEmailWhenMemberHasNoneInEvent() {
            // Arrange - minor with only guardian email
            LocalDate dateOfBirth = LocalDate.of(2010, 1, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM1003");
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            GuardianInformation guardian = new GuardianInformation(
                    "Parent",
                    "Name",
                    "PARENT",
                    EmailAddress.of("parent@example.com"),
                    PhoneNumber.of("+420777111222")
            );

            Member.RegisterMember command = new Member.RegisterMember(
                    new UserId(UUID.randomUUID()),
                    registrationNumber,
                    PersonalInformation.of("Anna", "Novakova", dateOfBirth, "CZ", Gender.FEMALE),
                    address,
                    null, null,
                    guardian,
                    BirthNumber.of("150102/1234"),
                    null
            );

            // Act
            Member member = Member.register(command);

            // Assert
            MemberCreatedEvent event = (MemberCreatedEvent) member.getDomainEvents().get(0);
            assertThat(event.getPrimaryEmail()).isEqualTo("parent@example.com");
        }


        @Test
        @DisplayName("should fail when minor has no guardian")
        void shouldFailWhenMinorHasNoGuardian() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.now().minusYears(15); // 15 years old
            int birthYear = dateOfBirth.getYear() % 100;
            RegistrationNumber registrationNumber = new RegistrationNumber(
                    String.format("ZBM%02d01", birthYear)
            );
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            EmailAddress email = new EmailAddress("anna@example.com");
            PhoneNumber phone = new PhoneNumber("+420111222333");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Anna",
                    "Nováková",
                    dateOfBirth,
                    "CZ",
                    Gender.FEMALE
            );

            UserId memberId = new UserId(UUID.randomUUID());

            // Act & Assert - register() validates business rules
            assertThatThrownBy(() -> {
                Member.RegisterMember command = new Member.RegisterMember(
                        memberId,
                        registrationNumber,
                        personalInformation,
                        address,
                        email,
                        phone,
                        null, // no guardian
                        null, // no birthNumber
                        null  // no bankAccountNumber
                );
                Member.register(command);
            })
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Guardian is required for minors");
        }

        @Test
        @DisplayName("should fail when no contact information provided")
        void shouldFailWhenNoContactInformationProvided() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE
            );
            UserId memberId = new UserId(UUID.randomUUID());

            // Act & Assert
            assertThatThrownBy(() -> {
                Member.RegisterMember command = new Member.RegisterMember(
                        memberId,
                        registrationNumber,
                        personalInformation,
                        address,
                        null, // no email
                        null, // no phone
                        null, // no guardian
                        null,
                        null
                );
                Member.register(command);
            })
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one email and one phone required");
        }

        @Test
        @DisplayName("should fail when email missing but phone provided")
        void shouldFailWhenEmailMissing() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            PhoneNumber phone = new PhoneNumber("+420123456789");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE
            );
            UserId memberId = new UserId(UUID.randomUUID());

            // Act & Assert
            assertThatThrownBy(() -> {
                Member.RegisterMember command = new Member.RegisterMember(
                        memberId,
                        registrationNumber,
                        personalInformation,
                        address,
                        null, // no email
                        phone,
                        null, // no guardian
                        null,
                        null
                );
                Member.register(command);
            })
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one email");
        }

        @Test
        @DisplayName("should fail when phone missing but email provided")
        void shouldFailWhenPhoneMissing() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            EmailAddress email = new EmailAddress("jan@example.com");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE
            );
            UserId memberId = new UserId(UUID.randomUUID());

            // Act & Assert
            assertThatThrownBy(() -> {
                Member.RegisterMember command = new Member.RegisterMember(
                        memberId,
                        registrationNumber,
                        personalInformation,
                        address,
                        email,
                        null, // no phone
                        null, // no guardian
                        null,
                        null
                );
                Member.register(command);
            })
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one phone");
        }

        @Test
        @DisplayName("should accept guardian contact for minor instead of member contact")
        void shouldAcceptGuardianContactForMinor() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.now().minusYears(12);
            int birthYear = dateOfBirth.getYear() % 100;
            RegistrationNumber registrationNumber = new RegistrationNumber(
                    String.format("ZBM%02d01", birthYear)
            );
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            GuardianInformation guardian = new GuardianInformation(
                    "Petr",
                    "Novák",
                    "PARENT",
                    EmailAddress.of("petr@example.com"),
                    PhoneNumber.of("+420999888777")
            );
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Anna",
                    "Nováková",
                    dateOfBirth,
                    "CZ",
                    Gender.FEMALE
            );
            UserId memberId = new UserId(UUID.randomUUID());

            // Act
            Member.RegisterMember command = new Member.RegisterMember(
                    memberId,
                    registrationNumber,
                    personalInformation,
                    address,
                    null, // no member email
                    null, // no member phone
                    guardian,
                    null,
                    null
            );
            Member member = Member.register(command);

            // Assert - should not throw
            MemberAssert.assertThat(member).hasGuardianNotNull();
        }

        @Test
        @DisplayName("should fail when first name is blank")
        void shouldFailWhenFirstNameIsBlank() {
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            EmailAddress email = new EmailAddress("test@example.com");
            PhoneNumber phone = new PhoneNumber("+420123456789");
            UserId memberId = new UserId(UUID.randomUUID());

            assertThatThrownBy(() -> {
                PersonalInformation personalInformation = PersonalInformation.of(
                        "",
                        "Novák",
                        LocalDate.of(1990, 5, 15),
                        "CZ",
                        Gender.MALE
                );
                Member.RegisterMember command = new Member.RegisterMember(
                        memberId,
                        registrationNumber,
                        personalInformation,
                        address,
                        email,
                        phone,
                        null,
                        null,
                        null
                );
                Member.register(command);
            })
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("First name");
        }

        @Test
        @DisplayName("should fail when last name is blank")
        void shouldFailWhenLastNameIsBlank() {
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            EmailAddress email = new EmailAddress("test@example.com");
            PhoneNumber phone = new PhoneNumber("+420123456789");
            UserId memberId = new UserId(UUID.randomUUID());

            assertThatThrownBy(() -> {
                PersonalInformation personalInformation = PersonalInformation.of(
                        "Jan",
                        "",
                        LocalDate.of(1990, 5, 15),
                        "CZ",
                        Gender.MALE
                );
                Member.RegisterMember command = new Member.RegisterMember(
                        memberId,
                        registrationNumber,
                        personalInformation,
                        address,
                        email,
                        phone,
                        null,
                        null,
                        null
                );
                Member.register(command);
            })
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Last name");
        }

        @Test
        @DisplayName("should fail when nationality is blank")
        void shouldFailWhenNationalityIsBlank() {
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            EmailAddress email = new EmailAddress("test@example.com");
            PhoneNumber phone = new PhoneNumber("+420123456789");
            UserId memberId = new UserId(UUID.randomUUID());

            assertThatThrownBy(() -> {
                PersonalInformation personalInformation = PersonalInformation.of(
                        "Jan",
                        "Novák",
                        LocalDate.of(1990, 5, 15),
                        "",
                        Gender.MALE
                );
                Member.RegisterMember command = new Member.RegisterMember(
                        memberId,
                        registrationNumber,
                        personalInformation,
                        address,
                        email,
                        phone,
                        null,
                        null,
                        null
                );
                Member.register(command);
            })
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nationality");
        }
    }

    private Member createTestMember() {
        return aMember()
                .withRegistrationNumber("ZBM9001")
                .withName("Jan", "Novák")
                .withDateOfBirth(LocalDate.of(1990, 5, 15))
                .withNationality("CZ")
                .withGender(Gender.MALE)
                .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                .withEmail("jan.novak@example.com")
                .withPhone("+420123456789")
                .withNoGuardian()
                .build();
    }

    @Nested
    @DisplayName("UpdateContactInformation command")
    class UpdateContactInformationCommand {

        @Test
        @DisplayName("should update email when provided")
        void shouldUpdateEmailWhenProvided() {
            // Arrange
            Member member = createTestMember();
            EmailAddress newEmail = EmailAddress.of("new.email@example.com");

            // Act - modifies member in-place, returns void
            member.handle(new Member.UpdateContactInformation(newEmail, null, null));

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
            member.handle(new Member.UpdateContactInformation(null, newPhone, null));

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
            member.handle(new Member.UpdateContactInformation(null, null, newAddress));

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
            member.handle(new Member.UpdateContactInformation(newEmail, newPhone, newAddress));

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
            member.handle(new Member.UpdateContactInformation(newEmail, null, null));

            // Assert - same instance is modified
            assertThat(member.getEmail().value()).isEqualTo("new.email@example.com");
        }

        @Test
        @DisplayName("should preserve other fields when updating contact info")
        void shouldPreserveOtherFields() {
            // Arrange
            Member member = createTestMember();
            MemberId originalId = member.getId();
            RegistrationNumber originalRegNum = member.getRegistrationNumber();
            String originalFirstName = member.getFirstName();
            String originalLastName = member.getLastName();
            LocalDate originalDob = member.getDateOfBirth();

            // Act
            member.handle(new Member.UpdateContactInformation(EmailAddress.of("new@example.com"), null, null));

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
            member.handle(new Member.UpdateContactInformation(null, null, null));

            // Assert - Email is preserved, not removed
            assertThat(member.getEmail()).isNotNull();
            assertThat(member.getEmail().value()).isEqualTo("jan.novak@example.com");
            assertThat(member.getPhone().value()).isEqualTo("+420123456789"); // unchanged
            assertThat(member.getAddress().street()).isEqualTo("Hlavní 123"); // unchanged
        }
    }

    @Nested
    @DisplayName("UpdateDocuments command")
    class UpdateDocumentsCommand {

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
            member.handle(new Member.UpdateDocuments(identityCard, null, null));

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
            member.handle(new Member.UpdateDocuments(null, medicalCourse, null));

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
            member.handle(new Member.UpdateDocuments(null, null, trainerLicense));

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
            member.handle(new Member.UpdateDocuments(identityCard, medicalCourse, trainerLicense));

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
            member.handle(new Member.UpdateDocuments(identityCard, null, null));

            // Assert - same instance is modified
            assertThat(member.getIdentityCard()).isNotNull();
        }

        @Test
        @DisplayName("should not validate existing documents when updating different document")
        void shouldNotValidateExistingDocumentsWhenUpdatingDifferentDocument() {
            // Arrange - Create member with a valid identity card
            LocalDate futureDate = LocalDate.now().plusYears(5);
            IdentityCard validIdentityCard = IdentityCard.of("VALID123", futureDate);

            Member memberWithDoc = MemberTestDataBuilder.aMember()
                    .withIdentityCard(validIdentityCard).build();

            MedicalCourse medicalCourse = MedicalCourse.of(
                    LocalDate.of(2023, 1, 15),
                    Optional.empty()
            );

            // Act - Update a different document (medical course, not identity card)
            // This should NOT trigger validation of the existing identity card
            memberWithDoc.handle(new Member.UpdateDocuments(null, medicalCourse, null));

            // Assert - Medical course is updated, identity card is unchanged
            assertThat(memberWithDoc.getMedicalCourse()).isNotNull();
            assertThat(memberWithDoc.getIdentityCard()).isNotNull();
            assertThat(memberWithDoc.getIdentityCard().cardNumber()).isEqualTo("VALID123"); // unchanged
            assertThat(memberWithDoc.getIdentityCard().validityDate()).isEqualTo(futureDate); // unchanged
        }
    }

    @Nested
    @DisplayName("UpdateMemberDetails command")
    class UpdateMemberDetailsCommand {

        @Test
        @DisplayName("should update gender when provided")
        void shouldUpdateGenderWhenProvided() {
            // Arrange
            Member member = createTestMember();

            // Act
            member.handle(new Member.UpdateMemberDetails(
                    null, null, null, null, null,
                    null, null, null, null, null, Gender.FEMALE
            ));

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
            member.handle(new Member.UpdateMemberDetails(
                    null, null, null, null, null,
                    "12345", null, null, null, null, null
            ));

            // Assert
            assertThat(member.getChipNumber()).isEqualTo("12345");
        }

        @Test
        @DisplayName("should update driving license group when provided")
        void shouldUpdateDrivingLicenseGroupWhenProvided() {
            // Arrange
            Member member = createTestMember();

            // Act
            member.handle(new Member.UpdateMemberDetails(
                    null, null, null, null, null,
                    null, DrivingLicenseGroup.A, null, null, null, null
            ));

            // Assert
            assertThat(member.getDrivingLicenseGroup()).isEqualTo(DrivingLicenseGroup.A);
        }

        @Test
        @DisplayName("should update dietary restrictions when provided")
        void shouldUpdateDietaryRestrictionsWhenProvided() {
            // Arrange
            Member member = createTestMember();

            // Act
            member.handle(new Member.UpdateMemberDetails(
                    null, null, null, null, null,
                    null, null, "Vegetarian, no nuts", null, null, null
            ));

            // Assert
            assertThat(member.getDietaryRestrictions()).isEqualTo("Vegetarian, no nuts");
        }

        @Test
        @DisplayName("should update multiple fields when multiple provided")
        void shouldUpdateMultipleFieldsWhenMultipleProvided() {
            // Arrange
            Member member = createTestMember();

            // Act
            member.handle(new Member.UpdateMemberDetails(
                    null, null, null, null, null,
                    "12345", DrivingLicenseGroup.A, "No dairy", null, null, Gender.FEMALE
            ));

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
            member.handle(new Member.UpdateMemberDetails(
                    newPersonalInfo, newAddress, newEmail, newPhone, null,
                    "999", DrivingLicenseGroup.B, "No gluten", null, null, null
            ));

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
            GuardianInformation guardian = new GuardianInformation(
                    "Jan",
                    "Novák",
                    "PARENT",
                    EmailAddress.of("jan.novak@example.com"),
                    PhoneNumber.of("+420111111111")
            );

            Member minor = aMember()
                    .withRegistrationNumber("ZBM9002")
                    .withName("Anna", "Nováková")
                    .withDateOfBirth(minorDob)
                    .withNationality("CZ")
                    .withGender(Gender.FEMALE)
                    .withAddress(createTestMember().getAddress())
                    .withEmail(createTestMember().getEmail())
                    .withPhone(createTestMember().getPhone())
                    .withGuardian(guardian)
                    .build();

            // Act
            minor.handle(new Member.UpdateMemberDetails(
                    null, null, null, null, null,
                    "123", null, "No nuts", null, null, null
            ));

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
            member.handle(new Member.UpdateMemberDetails(
                    null, null, null, null, null,
                    "12345", null, null, null, null, null
            ));

            // Assert - same instance is modified
            assertThat(member.getChipNumber()).isEqualTo("12345");
        }
    }

    @Nested
    @DisplayName("handle(TerminateMembership) method")
    class HandleTerminateMembership {

        private Member createActiveMember() {
            return aMember()
                    .withRegistrationNumber("ZBM9001")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .withNoGuardian()
                    .build();
        }

        @Test
        @DisplayName("active member should not have termination details")
        void activeMemberShouldNotHaveTerminationDetails() {
            // Arrange
            Member activeMember = createActiveMember();

            // Assert
            assertThat(activeMember.isActive()).isTrue();
            assertThat(activeMember.getDeactivationReason()).isNull();
            assertThat(activeMember.getDeactivatedAt()).isNull();
            assertThat(activeMember.getDeactivationNote()).isNull();
            assertThat(activeMember.getDeactivatedBy()).isNull();
        }

        @Test
        @DisplayName("should terminate active member with reason")
        void shouldTerminateActiveMemberWithReason() {
            // Arrange
            Member activeMember = createActiveMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            Member.TerminateMembership command = new Member.TerminateMembership(
                    adminUserId,
                    DeactivationReason.ODHLASKA,
                    "Member requested termination"
            );

            // Act
            activeMember.handle(command);

            // Assert
            assertThat(activeMember.isActive()).isFalse();
            assertThat(activeMember.getDeactivationReason()).isEqualTo(DeactivationReason.ODHLASKA);
            assertThat(activeMember.getDeactivatedAt()).isNotNull();
            assertThat(activeMember.getDeactivationNote()).isEqualTo("Member requested termination");
            assertThat(activeMember.getDeactivatedBy()).isEqualTo(adminUserId);
        }

        @Test
        @DisplayName("should publish MemberTerminatedEvent when terminating")
        void shouldPublishEventWhenTerminating() {
            // Arrange
            Member activeMember = createActiveMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            Member.TerminateMembership command = new Member.TerminateMembership(
                    adminUserId,
                    DeactivationReason.PRESTUP,
                    null
            );

            // Act
            // Clear creation event first, then terminate
            activeMember.clearDomainEvents();  // Clear the MemberCreatedEvent from registration
            activeMember.handle(command);

            // Assert - should have MemberTerminatedEvent
            assertThat(activeMember.getDomainEvents())
                    .hasSize(1)
                    .allMatch(event -> event instanceof MemberTerminatedEvent);
        }

        @Test
        @DisplayName("should reject termination of already terminated member")
        void shouldRejectTerminationOfAlreadyTerminatedMember() {
            // Arrange
            Member activeMember = createActiveMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            Member.TerminateMembership firstCommand = new Member.TerminateMembership(
                    adminUserId,
                    DeactivationReason.ODHLASKA,
                    "First termination"
            );
            activeMember.handle(firstCommand);

            UserId anotherAdmin = new UserId(UUID.randomUUID());
            Member.TerminateMembership secondCommand = new Member.TerminateMembership(
                    anotherAdmin,
                    DeactivationReason.OTHER,
                    "Second termination attempt"
            );

            // Act & Assert
            assertThatThrownBy(() -> activeMember.handle(secondCommand))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("already terminated");
        }

        @Test
        @DisplayName("should allow termination with all reason types")
        void shouldAllowTerminationWithAllReasonTypes() {
            // Arrange
            UserId adminUserId = new UserId(UUID.randomUUID());

            // Test ODHLASKA
            Member member1 = createActiveMember();
            member1.handle(new Member.TerminateMembership(
                    adminUserId, DeactivationReason.ODHLASKA, "Note 1"));
            assertThat(member1.getDeactivationReason()).isEqualTo(DeactivationReason.ODHLASKA);

            // Test PRESTUP
            Member member2 = createActiveMember();
            member2.handle(new Member.TerminateMembership(
                    adminUserId, DeactivationReason.PRESTUP, "Note 2"));
            assertThat(member2.getDeactivationReason()).isEqualTo(DeactivationReason.PRESTUP);

            // Test OTHER
            Member member3 = createActiveMember();
            member3.handle(new Member.TerminateMembership(
                    adminUserId, DeactivationReason.OTHER, "Note 3"));
            assertThat(member3.getDeactivationReason()).isEqualTo(DeactivationReason.OTHER);
        }

        @Test
        @DisplayName("should record termination timestamp")
        void shouldRecordTerminationTimestamp() {
            // Arrange
            Member activeMember = createActiveMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            var beforeTermination = System.currentTimeMillis();

            // Act
            activeMember.handle(new Member.TerminateMembership(
                    adminUserId, DeactivationReason.ODHLASKA, null));
            var afterTermination = System.currentTimeMillis();

            // Assert
            assertThat(activeMember.getDeactivatedAt()).isNotNull();
            assertThat(activeMember.getDeactivatedAt().toEpochMilli())
                    .isGreaterThanOrEqualTo(beforeTermination)
                    .isLessThanOrEqualTo(afterTermination);
        }
    }

    @Nested
    @DisplayName("handle(SelfUpdate) command")
    class HandleSelfUpdate {

        private Member createAdultMember() {
            return aMember()
                    .withRegistrationNumber("ZBM9001")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .withNoGuardian()
                    .build();
        }

        @Test
        @DisplayName("should update email when provided")
        void shouldUpdateEmailWhenProvided() {
            Member member = createAdultMember();
            EmailAddress newEmail = EmailAddress.of("new@example.com");

            member.handle(new Member.SelfUpdate(
                    newEmail, null, null, null, null, null,
                    null, null, null, null, null, null
            ));

            assertThat(member.getEmail()).isEqualTo(newEmail);
            assertThat(member.getPhone().value()).isEqualTo("+420123456789");
        }

        @Test
        @DisplayName("should update phone when provided")
        void shouldUpdatePhoneWhenProvided() {
            Member member = createAdultMember();
            PhoneNumber newPhone = PhoneNumber.of("+420999888777");

            member.handle(new Member.SelfUpdate(
                    null, newPhone, null, null, null, null,
                    null, null, null, null, null, null
            ));

            assertThat(member.getPhone()).isEqualTo(newPhone);
            assertThat(member.getEmail().value()).isEqualTo("jan.novak@example.com");
        }

        @Test
        @DisplayName("should update address when provided")
        void shouldUpdateAddressWhenProvided() {
            Member member = createAdultMember();
            Address newAddress = Address.of("Nová 1", "Brno", "60200", "CZ");

            member.handle(new Member.SelfUpdate(
                    null, null, newAddress, null, null, null,
                    null, null, null, null, null, null
            ));

            assertThat(member.getAddress().street()).isEqualTo("Nová 1");
        }

        @Test
        @DisplayName("should update chip number when provided")
        void shouldUpdateChipNumberWhenProvided() {
            Member member = createAdultMember();

            member.handle(new Member.SelfUpdate(
                    null, null, null, "99887", null, null,
                    null, null, null, null, null, null
            ));

            assertThat(member.getChipNumber()).isEqualTo("99887");
        }

        @Test
        @DisplayName("should update nationality when provided")
        void shouldUpdateNationalityWhenProvided() {
            Member member = createAdultMember();

            member.handle(new Member.SelfUpdate(
                    null, null, null, null, "SK", null,
                    null, null, null, null, null, null
            ));

            assertThat(member.getNationality()).isEqualTo("SK");
        }

        @Test
        @DisplayName("should update dietary restrictions when provided")
        void shouldUpdateDietaryRestrictionsWhenProvided() {
            Member member = createAdultMember();

            member.handle(new Member.SelfUpdate(
                    null, null, null, null, null, null,
                    null, null, null, null, "Vegan", null
            ));

            assertThat(member.getDietaryRestrictions()).isEqualTo("Vegan");
        }

        @Test
        @DisplayName("should not change firstName lastName dateOfBirth or gender")
        void shouldNotChangeAdminOnlyFields() {
            Member member = createAdultMember();

            member.handle(new Member.SelfUpdate(
                    EmailAddress.of("changed@example.com"), null, null, null, null, null,
                    null, null, null, null, null, null
            ));

            assertThat(member.getFirstName()).isEqualTo("Jan");
            assertThat(member.getLastName()).isEqualTo("Novák");
            assertThat(member.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
            assertThat(member.getGender()).isEqualTo(Gender.MALE);
        }

        @Test
        @DisplayName("should preserve existing values when null is passed")
        void shouldPreserveExistingValuesWhenNullPassed() {
            Member member = createAdultMember();

            member.handle(new Member.SelfUpdate(
                    null, null, null, null, null, null,
                    null, null, null, null, null, null
            ));

            assertThat(member.getEmail().value()).isEqualTo("jan.novak@example.com");
            assertThat(member.getPhone().value()).isEqualTo("+420123456789");
        }
    }

    @Nested
    @DisplayName("handle(UpdateMemberByAdmin) command")
    class HandleUpdateMemberByAdmin {

        private Member createAdultMember() {
            return aMember()
                    .withRegistrationNumber("ZBM9001")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .withNoGuardian()
                    .build();
        }

        @Test
        @DisplayName("should update firstName when provided")
        void shouldUpdateFirstNameWhenProvided() {
            Member member = createAdultMember();

            member.handle(new Member.UpdateMemberByAdmin(
                    null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    "Petr", null, null, null, null
            ));

            assertThat(member.getFirstName()).isEqualTo("Petr");
            assertThat(member.getLastName()).isEqualTo("Novák");
        }

        @Test
        @DisplayName("should update lastName when provided")
        void shouldUpdateLastNameWhenProvided() {
            Member member = createAdultMember();

            member.handle(new Member.UpdateMemberByAdmin(
                    null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, "Svoboda", null, null, null
            ));

            assertThat(member.getLastName()).isEqualTo("Svoboda");
            assertThat(member.getFirstName()).isEqualTo("Jan");
        }

        @Test
        @DisplayName("should update dateOfBirth when provided")
        void shouldUpdateDateOfBirthWhenProvided() {
            Member member = createAdultMember();
            LocalDate newDob = LocalDate.of(1985, 3, 20);

            member.handle(new Member.UpdateMemberByAdmin(
                    null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, newDob, null, null
            ));

            assertThat(member.getDateOfBirth()).isEqualTo(newDob);
        }

        @Test
        @DisplayName("should update gender when provided")
        void shouldUpdateGenderWhenProvided() {
            Member member = createAdultMember();

            member.handle(new Member.UpdateMemberByAdmin(
                    null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, Gender.FEMALE, null
            ));

            assertThat(member.getGender()).isEqualTo(Gender.FEMALE);
        }

        @Test
        @DisplayName("should update all self-editable fields when provided")
        void shouldUpdateAllSelfEditableFields() {
            Member member = createAdultMember();
            EmailAddress newEmail = EmailAddress.of("admin.set@example.com");
            PhoneNumber newPhone = PhoneNumber.of("+420111222333");

            member.handle(new Member.UpdateMemberByAdmin(
                    newEmail, newPhone, null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, null, null, null
            ));

            assertThat(member.getEmail()).isEqualTo(newEmail);
            assertThat(member.getPhone()).isEqualTo(newPhone);
        }

        @Test
        @DisplayName("should preserve unchanged fields when updating only admin-only fields")
        void shouldPreserveUnchangedFieldsWhenUpdatingAdminOnlyFields() {
            Member member = createAdultMember();

            member.handle(new Member.UpdateMemberByAdmin(
                    null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    "Petr", "Svoboda", null, null, null
            ));

            assertThat(member.getEmail().value()).isEqualTo("jan.novak@example.com");
            assertThat(member.getPhone().value()).isEqualTo("+420123456789");
            assertThat(member.getAddress().street()).isEqualTo("Hlavní 123");
        }

        @Test
        @DisplayName("should reject dateOfBirth update that makes member a minor without providing a guardian")
        void shouldRejectDateOfBirthUpdateThatMakesMemberMinorWithoutGuardian() {
            Member member = createAdultMember();
            LocalDate minorDateOfBirth = LocalDate.now().minusYears(10);

            assertThatThrownBy(() -> member.handle(new Member.UpdateMemberByAdmin(
                    null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, null, minorDateOfBirth, null, null
            ))).isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Guardian is required for minors");
        }
    }
}
