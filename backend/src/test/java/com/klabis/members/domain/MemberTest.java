package com.klabis.members.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberAssert;
import com.klabis.members.MemberCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

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
 */
@DisplayName("Member Aggregate")
class MemberTest {

    @Nested
    @DisplayName("create() method")
    class CreateMethod {

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

            // Act
            Member member = Member.create(
                    registrationNumber,
                    personalInformation,
                    address,
                    email,
                    phone,
                    null // no guardian for adult
            );

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
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Anna",
                    "Nováková",
                    dateOfBirth,
                    "CZ",
                    Gender.FEMALE
            );
            Address address = new Address("Školská 456", "Brno", "60200", "CZ");
            GuardianInformation guardian = new GuardianInformation(
                    "Petr",
                    "Novák",
                    "PARENT",
                    EmailAddress.of("petr.novak@example.com"),
                    PhoneNumber.of("+420987654321")
            );

            // Act
            Member member = Member.create(
                    registrationNumber,
                    personalInformation,
                    address,
                    null, // no member email
                    null, // no member phone
                    guardian
            );

            // Assert
            MemberAssert.assertThat(member)
                    .hasRegistrationNumber(registrationNumber)
                    .hasGuardianNotNull();
            assertThat(member.getGuardian().getFirstName()).isEqualTo("Petr");
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
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Anna",
                    "Nováková",
                    dateOfBirth,
                    "CZ",
                    Gender.FEMALE
            );
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            EmailAddress email = new EmailAddress("anna@example.com");
            PhoneNumber phone = new PhoneNumber("+420111222333");

            // Act & Assert
            assertThatThrownBy(() -> Member.create(
                    registrationNumber,
                    personalInformation,
                    address,
                    email,
                    phone,
                    null // no guardian
            ))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Guardian is required for minors");
        }

        @Test
        @DisplayName("should fail when no contact information provided")
        void shouldFailWhenNoContactInformationProvided() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE
            );
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");

            // Act & Assert
            assertThatThrownBy(() -> Member.create(
                    registrationNumber,
                    personalInformation,
                    address,
                    null, // no email
                    null, // no phone
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one email and one phone required");
        }

        @Test
        @DisplayName("should fail when email missing but phone provided")
        void shouldFailWhenEmailMissing() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE
            );
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            PhoneNumber phone = new PhoneNumber("+420123456789");

            // Act & Assert
            assertThatThrownBy(() -> Member.create(
                    registrationNumber,
                    personalInformation,
                    address,
                    null, // no email
                    phone,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one email");
        }

        @Test
        @DisplayName("should fail when phone missing but email provided")
        void shouldFailWhenPhoneMissing() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE
            );
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            EmailAddress email = new EmailAddress("jan@example.com");

            // Act & Assert
            assertThatThrownBy(() -> Member.create(
                    registrationNumber,
                    personalInformation,
                    address,
                    email,
                    null, // no phone
                    null
            ))
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
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Anna",
                    "Nováková",
                    dateOfBirth,
                    "CZ",
                    Gender.FEMALE
            );
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            GuardianInformation guardian = new GuardianInformation(
                    "Petr",
                    "Novák",
                    "PARENT",
                    EmailAddress.of("petr@example.com"),
                    PhoneNumber.of("+420999888777")
            );

            // Act
            Member member = Member.create(
                    registrationNumber,
                    personalInformation,
                    address,
                    null, // no member email
                    null, // no member phone
                    guardian // guardian has contacts
            );

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

            assertThatThrownBy(() -> {
                PersonalInformation personalInformation = PersonalInformation.of(
                        "",
                        "Novák",
                        LocalDate.of(1990, 5, 15),
                        "CZ",
                        Gender.MALE
                );
                Member.create(
                        registrationNumber,
                        personalInformation,
                        address,
                        email,
                        phone,
                        null
                );
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

            assertThatThrownBy(() -> {
                PersonalInformation personalInformation = PersonalInformation.of(
                        "Jan",
                        "",
                        LocalDate.of(1990, 5, 15),
                        "CZ",
                        Gender.MALE
                );
                Member.create(
                        registrationNumber,
                        personalInformation,
                        address,
                        email,
                        phone,
                        null
                );
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

            assertThatThrownBy(() -> {
                PersonalInformation personalInformation = PersonalInformation.of(
                        "Jan",
                        "Novák",
                        LocalDate.of(1990, 5, 15),
                        "",
                        Gender.MALE
                );
                Member.create(
                        registrationNumber,
                        personalInformation,
                        address,
                        email,
                        phone,
                        null
                );
            })
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nationality");
        }
    }

    @Nested
    @DisplayName("Domain Events (getDomainEvents, clearDomainEvents)")
    class DomainEvents {

        @Test
        @DisplayName("should register MemberCreatedEvent when member is created")
        void shouldRegisterMemberCreatedEventWhenCreated() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.of(2005, 6, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0501");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE
            );
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            EmailAddress email = new EmailAddress("jan@example.com");
            PhoneNumber phone = new PhoneNumber("+420777888999");

            // Act
            Member member = Member.create(
                    registrationNumber,
                    personalInformation,
                    address,
                    email,
                    phone,
                    null
            );

            // Assert
            List<Object> domainEvents = member.getDomainEvents();
            assertThat(domainEvents)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(MemberCreatedEvent.class);

            MemberCreatedEvent event = (MemberCreatedEvent) domainEvents.get(0);
            assertThat(event.getMemberId()).isEqualTo(member.getId());
            assertThat(event.getFirstName()).isEqualTo("Jan");
            assertThat(event.getLastName()).isEqualTo("Novák");
            assertThat(event.getDateOfBirth()).isEqualTo(dateOfBirth);
            assertThat(event.getNationality()).isEqualTo("CZ");
            assertThat(event.getGender()).isEqualTo(Gender.MALE);
            assertThat(event.getRegistrationNumber()).isEqualTo(registrationNumber);
            assertThat(event.getAddress()).isEqualTo(address);
            assertThat(event.getEmail()).isPresent().contains(email);
            assertThat(event.getPhone()).isPresent().contains(phone);
        }

        @Test
        @DisplayName("should include guardian information in MemberCreatedEvent for minors")
        void shouldIncludeGuardianInEventForMinors() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.of(2010, 1, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM1001");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Anna",
                    "Nováková",
                    dateOfBirth,
                    "CZ",
                    Gender.FEMALE
            );
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            EmailAddress email = new EmailAddress("child@example.com");
            PhoneNumber phone = new PhoneNumber("+420777333444");
            GuardianInformation guardian = new GuardianInformation(
                    "Parent",
                    "Name",
                    "PARENT",
                    EmailAddress.of("parent@example.com"),
                    PhoneNumber.of("+420777111222")
            );

            // Act
            Member member = Member.create(
                    registrationNumber,
                    personalInformation,
                    address,
                    email,
                    phone,
                    guardian
            );

            // Assert
            List<Object> domainEvents = member.getDomainEvents();
            assertThat(domainEvents).hasSize(1);

            MemberCreatedEvent event = (MemberCreatedEvent) domainEvents.get(0);
            assertThat(event.getGuardian()).isNotNull();
            assertThat(event.getGuardian().getFirstName()).isEqualTo("Parent");
            assertThat(event.getGuardian().getLastName()).isEqualTo("Name");
            assertThat(event.getGuardian().getRelationship()).isEqualTo("PARENT");
            assertThat(event.isMinor()).isTrue();
        }

        @Test
        @DisplayName("should include email and phone in MemberCreatedEvent")
        void shouldIncludeContactsInEvent() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.of(2005, 6, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0501");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE
            );
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            EmailAddress email = new EmailAddress("primary@example.com");
            PhoneNumber phone = new PhoneNumber("+420777111222");

            // Act
            Member member = Member.create(
                    registrationNumber,
                    personalInformation,
                    address,
                    email,
                    phone,
                    null
            );

            // Assert
            MemberCreatedEvent event = (MemberCreatedEvent) member.getDomainEvents().get(0);
            assertThat(event.getEmail()).isPresent().contains(email);
            assertThat(event.getPhone()).isPresent().contains(phone);
        }

        @Test
        @DisplayName("should return member email as primary email in MemberCreatedEvent")
        void shouldReturnMemberEmailAsPrimaryInEvent() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.of(2005, 6, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0503");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE
            );
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            EmailAddress email = new EmailAddress("test@example.com");
            PhoneNumber phone = new PhoneNumber("+420777888999");

            // Act
            Member member = Member.create(
                    registrationNumber,
                    personalInformation,
                    address,
                    email,
                    phone,
                    null
            );

            // Assert
            MemberCreatedEvent event = (MemberCreatedEvent) member.getDomainEvents().get(0);
            assertThat(event.getPrimaryEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should prefer member email over guardian email as primary in MemberCreatedEvent")
        void shouldPreferMemberEmailOverGuardianEmailInEvent() {
            // Arrange - minor with both member and guardian emails
            LocalDate dateOfBirth = LocalDate.of(2010, 1, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM1002");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Child",
                    "Minor",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE
            );
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            EmailAddress email = new EmailAddress("child@example.com");
            PhoneNumber phone = new PhoneNumber("+420777333444");
            GuardianInformation guardian = new GuardianInformation(
                    "Parent",
                    "Name",
                    "PARENT",
                    EmailAddress.of("parent@example.com"),
                    PhoneNumber.of("+420777111222")
            );

            // Act
            Member member = Member.create(
                    registrationNumber,
                    personalInformation,
                    address,
                    email,
                    phone,
                    guardian
            );

            // Assert
            MemberCreatedEvent event = (MemberCreatedEvent) member.getDomainEvents().get(0);
            assertThat(event.getPrimaryEmail()).isEqualTo("child@example.com");
        }

        @Test
        @DisplayName("should use guardian email as primary when member has no email in MemberCreatedEvent")
        void shouldUseGuardianEmailWhenMemberHasNoneInEvent() {
            // Arrange - minor with only guardian email
            LocalDate dateOfBirth = LocalDate.of(2010, 1, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM1003");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Child",
                    "Minor",
                    dateOfBirth,
                    "CZ",
                    Gender.FEMALE
            );
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            GuardianInformation guardian = new GuardianInformation(
                    "Parent",
                    "Name",
                    "PARENT",
                    EmailAddress.of("parent@example.com"),
                    PhoneNumber.of("+420777111222")
            );

            // Act
            Member member = Member.create(
                    registrationNumber,
                    personalInformation,
                    address,
                    null, // no member email
                    null, // no member phone
                    guardian
            );

            // Assert
            MemberCreatedEvent event = (MemberCreatedEvent) member.getDomainEvents().get(0);
            assertThat(event.getPrimaryEmail()).isEqualTo("parent@example.com");
        }

        @Test
        @DisplayName("should clear domain events after clearDomainEvents is called")
        void shouldClearDomainEventsAfterClear() {
            // Arrange
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Clear",
                    "Test",
                    LocalDate.of(2005, 6, 15),
                    "CZ",
                    Gender.MALE
            );
            Address address = new Address("Ulice 1", "Město", "11000", "CZ");
            EmailAddress email = new EmailAddress("clear@example.com");
            PhoneNumber phone = new PhoneNumber("+420777555666");

            Member member = Member.create(
                    new RegistrationNumber("ZBM0504"),
                    personalInformation,
                    address,
                    email,
                    phone,
                    null
            );

            assertThat(member.getDomainEvents()).hasSize(1);

            // Act
            member.clearDomainEvents();

            // Assert
            assertThat(member.getDomainEvents()).isEmpty();
        }
    }
}
