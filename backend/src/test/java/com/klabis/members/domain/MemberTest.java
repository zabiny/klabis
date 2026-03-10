package com.klabis.members.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.MemberAssert;
import com.klabis.members.MemberCreatedEvent;
import com.klabis.members.MemberResumedEvent;
import com.klabis.members.MemberSuspendedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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
            MemberId memberId = new MemberId(UUID.randomUUID());

            // Act
            Member.RegisterMember command = MemberRegisterMemberBuilder.builder()
                    .id(memberId)
                    .registrationNumber(registrationNumber)
                    .personalInformation(personalInformation)
                    .address(address)
                    .email(email)
                    .phone(phone)
                    .build();
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
            assertThat(event.eventId()).isNotNull();
            assertThat(event.memberId()).isEqualTo(member.getId());
            assertThat(event.registrationNumber()).isEqualTo(registrationNumber);
            assertThat(event.firstName()).isEqualTo("Jan");
            assertThat(event.lastName()).isEqualTo("Novák");
            assertThat(event.dateOfBirth()).isEqualTo(dateOfBirth);
            assertThat(event.nationality()).isEqualTo("CZ");
            assertThat(event.gender()).isEqualTo(Gender.MALE);
            assertThat(event.address()).isEqualTo(address);
            assertThat(event.emailAsOptional()).isPresent().contains(email);
            assertThat(event.phoneAsOptional()).isPresent().contains(phone);
            assertThat(event.guardian()).isNull();
            assertThat(event.isMinor()).isFalse();
            assertThat(event.getPrimaryEmail()).isEqualTo("jan.novak@example.com");
            assertThat(event.occurredAt()).isNotNull();

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
            MemberId memberId = new MemberId(UUID.randomUUID());

            // Act
            Member.RegisterMember command = MemberRegisterMemberBuilder.builder()
                    .id(memberId)
                    .registrationNumber(registrationNumber)
                    .personalInformation(personalInformation)
                    .address(address)
                    .guardian(guardian)
                    .build();
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
            assertThat(event.eventId()).isNotNull();
            assertThat(event.memberId()).isEqualTo(member.getId());
            assertThat(event.registrationNumber()).isEqualTo(registrationNumber);
            assertThat(event.firstName()).isEqualTo("Anna");
            assertThat(event.lastName()).isEqualTo("Nováková");
            assertThat(event.dateOfBirth()).isEqualTo(dateOfBirth);
            assertThat(event.nationality()).isEqualTo("CZ");
            assertThat(event.gender()).isEqualTo(Gender.FEMALE);
            assertThat(event.address()).isEqualTo(address);
            assertThat(event.emailAsOptional()).isEmpty();
            assertThat(event.phoneAsOptional()).isEmpty();
            assertThat(event.guardian()).isNotNull();
            assertThat(event.guardian().getFirstName()).isEqualTo("Petr");
            assertThat(event.guardian().getLastName()).isEqualTo("Novák");
            assertThat(event.guardian().getRelationship()).isEqualTo("PARENT");
            assertThat(event.guardian().getEmail()).isEqualTo(EmailAddress.of("petr.novak@example.com"));
            assertThat(event.guardian().getPhone()).isEqualTo(PhoneNumber.of("+420987654321"));
            assertThat(event.isMinor()).isTrue();
            assertThat(event.getPrimaryEmail()).isEqualTo("petr.novak@example.com");
            assertThat(event.occurredAt()).isNotNull();

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

            Member.RegisterMember command = MemberRegisterMemberBuilder.builder()
                    .id(new MemberId(UUID.randomUUID()))
                    .registrationNumber(registrationNumber)
                    .personalInformation(PersonalInformation.of("Anna", "Novakova", dateOfBirth, "CZ", Gender.FEMALE))
                    .address(address)
                    .guardian(guardian)
                    .birthNumber(BirthNumber.of("150102/1234"))
                    .build();

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

            MemberId memberId = new MemberId(UUID.randomUUID());

            // Act & Assert - register() validates business rules
            assertThatThrownBy(() -> {
                Member.RegisterMember command = MemberRegisterMemberBuilder.builder()
                        .id(memberId)
                        .registrationNumber(registrationNumber)
                        .personalInformation(personalInformation)
                        .address(address)
                        .email(email)
                        .phone(phone)
                        .build();
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
            MemberId memberId = new MemberId(UUID.randomUUID());

            // Act & Assert
            assertThatThrownBy(() -> {
                Member.RegisterMember command = MemberRegisterMemberBuilder.builder()
                        .id(memberId)
                        .registrationNumber(registrationNumber)
                        .personalInformation(personalInformation)
                        .address(address)
                        .build();
                Member.register(command);
            })
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one email address is required (member or guardian)");
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
            MemberId memberId = new MemberId(UUID.randomUUID());

            // Act & Assert
            assertThatThrownBy(() -> {
                Member.RegisterMember command = MemberRegisterMemberBuilder.builder()
                        .id(memberId)
                        .registrationNumber(registrationNumber)
                        .personalInformation(personalInformation)
                        .address(address)
                        .phone(phone)
                        .build();
                Member.register(command);
            })
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one email address is required (member or guardian)");
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
            MemberId memberId = new MemberId(UUID.randomUUID());

            // Act & Assert
            assertThatThrownBy(() -> {
                Member.RegisterMember command = MemberRegisterMemberBuilder.builder()
                        .id(memberId)
                        .registrationNumber(registrationNumber)
                        .personalInformation(personalInformation)
                        .address(address)
                        .email(email)
                        .build();
                Member.register(command);
            })
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one phone number is required (member or guardian)");
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
            MemberId memberId = new MemberId(UUID.randomUUID());

            // Act
            Member.RegisterMember command = MemberRegisterMemberBuilder.builder()
                    .id(memberId)
                    .registrationNumber(registrationNumber)
                    .personalInformation(personalInformation)
                    .address(address)
                    .guardian(guardian)
                    .build();
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
            MemberId memberId = new MemberId(UUID.randomUUID());

            assertThatThrownBy(() -> {
                PersonalInformation personalInformation = PersonalInformation.of(
                        "",
                        "Novák",
                        LocalDate.of(1990, 5, 15),
                        "CZ",
                        Gender.MALE
                );
                Member.RegisterMember command = MemberRegisterMemberBuilder.builder()
                        .id(memberId)
                        .registrationNumber(registrationNumber)
                        .personalInformation(personalInformation)
                        .address(address)
                        .email(email)
                        .phone(phone)
                        .build();
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
            MemberId memberId = new MemberId(UUID.randomUUID());

            assertThatThrownBy(() -> {
                PersonalInformation personalInformation = PersonalInformation.of(
                        "Jan",
                        "",
                        LocalDate.of(1990, 5, 15),
                        "CZ",
                        Gender.MALE
                );
                Member.RegisterMember command = MemberRegisterMemberBuilder.builder()
                        .id(memberId)
                        .registrationNumber(registrationNumber)
                        .personalInformation(personalInformation)
                        .address(address)
                        .email(email)
                        .phone(phone)
                        .build();
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
            MemberId memberId = new MemberId(UUID.randomUUID());

            assertThatThrownBy(() -> {
                PersonalInformation personalInformation = PersonalInformation.of(
                        "Jan",
                        "Novák",
                        LocalDate.of(1990, 5, 15),
                        "",
                        Gender.MALE
                );
                Member.RegisterMember command = MemberRegisterMemberBuilder.builder()
                        .id(memberId)
                        .registrationNumber(registrationNumber)
                        .personalInformation(personalInformation)
                        .address(address)
                        .email(email)
                        .phone(phone)
                        .build();
                Member.register(command);
            })
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Nationality");
        }
    }

    @Nested
    @DisplayName("handle(SuspendMembership) method")
    class HandleSuspendMembership {

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
        @DisplayName("active member should not have suspension details")
        void activeMemberShouldNotHaveSuspensionDetails() {
            // Arrange
            Member activeMember = createActiveMember();

            // Assert
            assertThat(activeMember.isActive()).isTrue();
            assertThat(activeMember.getSuspensionReason()).isNull();
            assertThat(activeMember.getSuspendedAt()).isNull();
            assertThat(activeMember.getSuspensionNote()).isNull();
            assertThat(activeMember.getSuspendedBy()).isNull();
        }

        @Test
        @DisplayName("should suspend active member with reason")
        void shouldSuspendActiveMemberWithReason() {
            // Arrange
            Member activeMember = createActiveMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            Member.SuspendMembership command = new Member.SuspendMembership(
                    adminUserId,
                    DeactivationReason.ODHLASKA,
                    "Member requested termination"
            );

            // Act
            activeMember.handle(command);

            // Assert
            assertThat(activeMember.isActive()).isFalse();
            assertThat(activeMember.getSuspensionReason()).isEqualTo(DeactivationReason.ODHLASKA);
            assertThat(activeMember.getSuspendedAt()).isNotNull();
            assertThat(activeMember.getSuspensionNote()).isEqualTo("Member requested termination");
            assertThat(activeMember.getSuspendedBy()).isEqualTo(adminUserId);
        }

        @Test
        @DisplayName("should publish MemberSuspendedEvent when suspending")
        void shouldPublishEventWhenSuspending() {
            // Arrange
            Member activeMember = createActiveMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            Member.SuspendMembership command = new Member.SuspendMembership(
                    adminUserId,
                    DeactivationReason.PRESTUP,
                    null
            );

            // Act
            // Clear creation event first, then suspend
            activeMember.clearDomainEvents();  // Clear the MemberCreatedEvent from registration
            activeMember.handle(command);

            // Assert - should have MemberSuspendedEvent
            assertThat(activeMember.getDomainEvents())
                    .hasSize(1)
                    .allMatch(event -> event instanceof MemberSuspendedEvent);
        }

        @Test
        @DisplayName("should reject suspension of already suspended member")
        void shouldRejectSuspensionOfAlreadySuspendedMember() {
            // Arrange
            Member activeMember = createActiveMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            Member.SuspendMembership firstCommand = new Member.SuspendMembership(
                    adminUserId,
                    DeactivationReason.ODHLASKA,
                    "First termination"
            );
            activeMember.handle(firstCommand);

            UserId anotherAdmin = new UserId(UUID.randomUUID());
            Member.SuspendMembership secondCommand = new Member.SuspendMembership(
                    anotherAdmin,
                    DeactivationReason.OTHER,
                    "Second termination attempt"
            );

            // Act & Assert
            assertThatThrownBy(() -> activeMember.handle(secondCommand))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("already suspended");
        }

        @Test
        @DisplayName("should allow suspension with all reason types")
        void shouldAllowSuspensionWithAllReasonTypes() {
            // Arrange
            UserId adminUserId = new UserId(UUID.randomUUID());

            // Test ODHLASKA
            Member member1 = createActiveMember();
            member1.handle(new Member.SuspendMembership(
                    adminUserId, DeactivationReason.ODHLASKA, "Note 1"));
            assertThat(member1.getSuspensionReason()).isEqualTo(DeactivationReason.ODHLASKA);

            // Test PRESTUP
            Member member2 = createActiveMember();
            member2.handle(new Member.SuspendMembership(
                    adminUserId, DeactivationReason.PRESTUP, "Note 2"));
            assertThat(member2.getSuspensionReason()).isEqualTo(DeactivationReason.PRESTUP);

            // Test OTHER
            Member member3 = createActiveMember();
            member3.handle(new Member.SuspendMembership(
                    adminUserId, DeactivationReason.OTHER, "Note 3"));
            assertThat(member3.getSuspensionReason()).isEqualTo(DeactivationReason.OTHER);
        }

        @Test
        @DisplayName("should record suspension timestamp")
        void shouldRecordSuspensionTimestamp() {
            // Arrange
            Member activeMember = createActiveMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            var beforeSuspension = System.currentTimeMillis();

            // Act
            activeMember.handle(new Member.SuspendMembership(
                    adminUserId, DeactivationReason.ODHLASKA, null));
            var afterSuspension = System.currentTimeMillis();

            // Assert
            assertThat(activeMember.getSuspendedAt()).isNotNull();
            assertThat(activeMember.getSuspendedAt().toEpochMilli())
                    .isGreaterThanOrEqualTo(beforeSuspension)
                    .isLessThanOrEqualTo(afterSuspension);
        }
    }

    @Nested
    @DisplayName("handle(ResumeMembership) method")
    class HandleResumeMembership {

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

        private Member createSuspendedMember() {
            Member activeMember = createActiveMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            Member.SuspendMembership suspendCommand = new Member.SuspendMembership(
                    adminUserId,
                    DeactivationReason.ODHLASKA,
                    "Member requested termination"
            );
            activeMember.handle(suspendCommand);
            return activeMember;
        }

        @Test
        @DisplayName("should resume suspended member")
        void shouldResumeSuspendedMember() {
            // Arrange
            Member suspendedMember = createSuspendedMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            Member.ResumeMembership command = new Member.ResumeMembership(adminUserId);

            // Act
            suspendedMember.handle(command);

            // Assert
            assertThat(suspendedMember.isActive()).isTrue();
            assertThat(suspendedMember.getSuspensionReason()).isNull();
            assertThat(suspendedMember.getSuspendedAt()).isNull();
            assertThat(suspendedMember.getSuspensionNote()).isNull();
            assertThat(suspendedMember.getSuspendedBy()).isNull();
        }

        @Test
        @DisplayName("should publish MemberResumedEvent when resuming")
        void shouldPublishEventWhenResuming() {
            // Arrange
            Member suspendedMember = createSuspendedMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            Member.ResumeMembership command = new Member.ResumeMembership(adminUserId);

            // Act - clear creation event first, then resume
            suspendedMember.clearDomainEvents();
            suspendedMember.handle(command);

            // Assert - should have MemberResumedEvent
            assertThat(suspendedMember.getDomainEvents())
                    .hasSize(1)
                    .allMatch(event -> event instanceof MemberResumedEvent);
        }

        @Test
        @DisplayName("should reject resume of already active member")
        void shouldRejectResumeOfAlreadyActiveMember() {
            // Arrange
            Member activeMember = createActiveMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            Member.ResumeMembership command = new Member.ResumeMembership(adminUserId);

            // Act & Assert
            assertThatThrownBy(() -> activeMember.handle(command))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("already active");
        }

        @Test
        @DisplayName("should record resume timestamp in event")
        void shouldRecordResumeTimestampInEvent() {
            // Arrange
            Member suspendedMember = createSuspendedMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            Member.ResumeMembership command = new Member.ResumeMembership(adminUserId);

            var beforeResume = System.currentTimeMillis();

            // Act
            suspendedMember.clearDomainEvents();
            suspendedMember.handle(command);

            var afterResume = System.currentTimeMillis();

            // Assert
            assertThat(suspendedMember.getDomainEvents())
                    .hasSize(1)
                    .allMatch(event -> event instanceof MemberResumedEvent);

            MemberResumedEvent event = (MemberResumedEvent) suspendedMember.getDomainEvents().get(0);
            assertThat(event.resumedAt()).isNotNull();
            assertThat(event.resumedAt().toEpochMilli())
                    .isGreaterThanOrEqualTo(beforeResume)
                    .isLessThanOrEqualTo(afterResume);
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

            member.handle(MemberSelfUpdateBuilder.builder().email(newEmail).build());

            assertThat(member.getEmail()).isEqualTo(newEmail);
            assertThat(member.getPhone().value()).isEqualTo("+420123456789");
        }

        @Test
        @DisplayName("should update phone when provided")
        void shouldUpdatePhoneWhenProvided() {
            Member member = createAdultMember();
            PhoneNumber newPhone = PhoneNumber.of("+420999888777");

            member.handle(MemberSelfUpdateBuilder.builder().phone(newPhone).build());

            assertThat(member.getPhone()).isEqualTo(newPhone);
            assertThat(member.getEmail().value()).isEqualTo("jan.novak@example.com");
        }

        @Test
        @DisplayName("should update address when provided")
        void shouldUpdateAddressWhenProvided() {
            Member member = createAdultMember();
            Address newAddress = Address.of("Nová 1", "Brno", "60200", "CZ");

            member.handle(MemberSelfUpdateBuilder.builder().address(newAddress).build());

            assertThat(member.getAddress().street()).isEqualTo("Nová 1");
        }

        @Test
        @DisplayName("should update chip number when provided")
        void shouldUpdateChipNumberWhenProvided() {
            Member member = createAdultMember();

            member.handle(MemberSelfUpdateBuilder.builder().chipNumber("99887").build());

            assertThat(member.getChipNumber()).isEqualTo("99887");
        }

        @Test
        @DisplayName("should update nationality when provided")
        void shouldUpdateNationalityWhenProvided() {
            Member member = createAdultMember();

            member.handle(MemberSelfUpdateBuilder.builder().nationality("SK").build());

            assertThat(member.getNationality()).isEqualTo("SK");
        }

        @Test
        @DisplayName("should update dietary restrictions when provided")
        void shouldUpdateDietaryRestrictionsWhenProvided() {
            Member member = createAdultMember();

            member.handle(MemberSelfUpdateBuilder.builder().dietaryRestrictions("Vegan").build());

            assertThat(member.getDietaryRestrictions()).isEqualTo("Vegan");
        }

        @Test
        @DisplayName("should not change firstName lastName dateOfBirth or gender")
        void shouldNotChangeAdminOnlyFields() {
            Member member = createAdultMember();

            member.handle(MemberSelfUpdateBuilder.builder()
                    .email(EmailAddress.of("changed@example.com"))
                    .build());

            assertThat(member.getFirstName()).isEqualTo("Jan");
            assertThat(member.getLastName()).isEqualTo("Novák");
            assertThat(member.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
            assertThat(member.getGender()).isEqualTo(Gender.MALE);
        }

        @Test
        @DisplayName("should preserve existing values when null is passed")
        void shouldPreserveExistingValuesWhenNullPassed() {
            Member member = createAdultMember();

            member.handle(MemberSelfUpdateBuilder.builder().build());

            assertThat(member.getEmail().value()).isEqualTo("jan.novak@example.com");
            assertThat(member.getPhone().value()).isEqualTo("+420123456789");
        }

        @Test
        @DisplayName("should reject self-update on minor without guardian")
        void shouldRejectSelfUpdateOnMinorWithoutGuardian() {
            Member minorWithoutGuardian = aMember()
                    .withRegistrationNumber("ZBM1002")
                    .withName("Anna", "Malá")
                    .withDateOfBirth(LocalDate.now().minusYears(12))
                    .withNationality("CZ")
                    .withGender(Gender.FEMALE)
                    .withAddress(Address.of("Dětská 2", "Praha", "11000", "CZ"))
                    .withEmail("guardian@example.com")
                    .withPhone("+420222000222")
                    .withNoGuardian()
                    .build();

            assertThatThrownBy(() -> minorWithoutGuardian.handle(MemberSelfUpdateBuilder.builder()
                    .address(Address.of("Nová 5", "Brno", "60200", "CZ"))
                    .build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Guardian is required for minors");
        }

        @Test
        @DisplayName("should clear birth number when nationality changes from CZE to non-Czech")
        void shouldClearBirthNumberWhenNationalityChangesFromCZEToNonCzech() {
            Member member = aMember()
                    .withRegistrationNumber("ZBM9002")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withNationality("CZE")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .withBirthNumber("9005151234")
                    .withNoGuardian()
                    .build();

            assertThat(member.getBirthNumber()).isNotNull();

            member.handle(MemberSelfUpdateBuilder.builder().nationality("SK").build());

            assertThat(member.getNationality()).isEqualTo("SK");
            assertThat(member.getBirthNumber()).isNull();
        }

        @Test
        @DisplayName("should preserve birth number when nationality changes between CZ and CZE")
        void shouldPreserveBirthNumberWhenNationalityChangesBetweenCZAndCZE() {
            Member memberWithCZ = aMember()
                    .withRegistrationNumber("ZBM9003")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .withBirthNumber("9005151234")
                    .withNoGuardian()
                    .build();

            memberWithCZ.handle(MemberSelfUpdateBuilder.builder().nationality("CZE").build());

            assertThat(memberWithCZ.getNationality()).isEqualTo("CZE");
            assertThat(memberWithCZ.getBirthNumber()).isNotNull();
        }

        @Test
        @DisplayName("should clear birth number when nationality changes from CZ to non-CZ")
        void shouldClearBirthNumberWhenNationalityChangesFromCZToNonCZ() {
            Member member = aMember()
                    .withRegistrationNumber("ZBM9001")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .withBirthNumber("9005151234")
                    .withNoGuardian()
                    .build();

            assertThat(member.getBirthNumber()).isNotNull();

            member.handle(MemberSelfUpdateBuilder.builder().nationality("SK").build());

            assertThat(member.getNationality()).isEqualTo("SK");
            assertThat(member.getBirthNumber()).isNull();
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

            member.handle(MemberUpdateMemberByAdminBuilder.builder().firstName("Petr").build());

            assertThat(member.getFirstName()).isEqualTo("Petr");
            assertThat(member.getLastName()).isEqualTo("Novák");
        }

        @Test
        @DisplayName("should update lastName when provided")
        void shouldUpdateLastNameWhenProvided() {
            Member member = createAdultMember();

            member.handle(MemberUpdateMemberByAdminBuilder.builder().lastName("Svoboda").build());

            assertThat(member.getLastName()).isEqualTo("Svoboda");
            assertThat(member.getFirstName()).isEqualTo("Jan");
        }

        @Test
        @DisplayName("should update dateOfBirth when provided")
        void shouldUpdateDateOfBirthWhenProvided() {
            Member member = createAdultMember();
            LocalDate newDob = LocalDate.of(1985, 3, 20);

            member.handle(MemberUpdateMemberByAdminBuilder.builder().dateOfBirth(newDob).build());

            assertThat(member.getDateOfBirth()).isEqualTo(newDob);
        }

        @Test
        @DisplayName("should update gender when provided")
        void shouldUpdateGenderWhenProvided() {
            Member member = createAdultMember();

            member.handle(MemberUpdateMemberByAdminBuilder.builder().gender(Gender.FEMALE).build());

            assertThat(member.getGender()).isEqualTo(Gender.FEMALE);
        }

        @Test
        @DisplayName("should update all self-editable fields when provided")
        void shouldUpdateAllSelfEditableFields() {
            Member member = createAdultMember();
            EmailAddress newEmail = EmailAddress.of("admin.set@example.com");
            PhoneNumber newPhone = PhoneNumber.of("+420111222333");

            member.handle(MemberUpdateMemberByAdminBuilder.builder()
                    .email(newEmail)
                    .phone(newPhone)
                    .build());

            assertThat(member.getEmail()).isEqualTo(newEmail);
            assertThat(member.getPhone()).isEqualTo(newPhone);
        }

        @Test
        @DisplayName("should preserve unchanged fields when updating only admin-only fields")
        void shouldPreserveUnchangedFieldsWhenUpdatingAdminOnlyFields() {
            Member member = createAdultMember();

            member.handle(MemberUpdateMemberByAdminBuilder.builder()
                    .firstName("Petr")
                    .lastName("Svoboda")
                    .build());

            assertThat(member.getEmail().value()).isEqualTo("jan.novak@example.com");
            assertThat(member.getPhone().value()).isEqualTo("+420123456789");
            assertThat(member.getAddress().street()).isEqualTo("Hlavní 123");
        }

        @Test
        @DisplayName("should reject dateOfBirth update that makes member a minor without providing a guardian")
        void shouldRejectDateOfBirthUpdateThatMakesMemberMinorWithoutGuardian() {
            Member member = createAdultMember();
            LocalDate minorDateOfBirth = LocalDate.now().minusYears(10);

            assertThatThrownBy(() -> member.handle(MemberUpdateMemberByAdminBuilder.builder()
                    .dateOfBirth(minorDateOfBirth)
                    .build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Guardian is required for minors");
        }

        @Test
        @DisplayName("should reject setting birth number on non-Czech member")
        void shouldRejectSettingBirthNumberOnNonCzechMember() {
            Member member = aMember()
                    .withRegistrationNumber("ZBM9001")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withNationality("SK")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Bratislava", "81102", "SK"))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .withNoGuardian()
                    .build();

            assertThatThrownBy(() -> member.handle(MemberUpdateMemberByAdminBuilder.builder()
                    .birthNumber(BirthNumber.of("9005151234"))
                    .build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Birth number is only allowed for Czech nationals");
        }

        @Test
        @DisplayName("should allow setting birth number on Czech member")
        void shouldAllowSettingBirthNumberOnCzechMember() {
            Member member = createAdultMember();

            member.handle(MemberUpdateMemberByAdminBuilder.builder()
                    .birthNumber(BirthNumber.of("9005151234"))
                    .build());

            assertThat(member.getBirthNumber()).isNotNull();
        }

        @Test
        @DisplayName("should allow update when guardian provides email coverage")
        void shouldAllowUpdateWhenGuardianProvidesEmailCoverage() {
            Member member = createAdultMember();

            member.handle(MemberUpdateMemberByAdminBuilder.builder()
                    .email(EmailAddress.of("temp@example.com"))
                    .guardian(new GuardianInformation("Jane", "Doe", "PARENT",
                            EmailAddress.of("jane@example.com"), PhoneNumber.of("+420111222333")))
                    .build());

            assertThat(member.getEmail()).isEqualTo(EmailAddress.of("temp@example.com"));
        }

        @Test
        @DisplayName("should reject unrelated admin update on existing minor without guardian")
        void shouldRejectUnrelatedAdminUpdateOnExistingMinorWithoutGuardian() {
            // Minor reconstructed without guardian (data integrity gap from legacy import)
            Member minorWithoutGuardian = aMember()
                    .withRegistrationNumber("ZBM1001")
                    .withName("Jana", "Malá")
                    .withDateOfBirth(LocalDate.now().minusYears(10))
                    .withNationality("CZ")
                    .withGender(Gender.FEMALE)
                    .withAddress(Address.of("Dětská 1", "Praha", "11000", "CZ"))
                    .withEmail("guardian@example.com")
                    .withPhone("+420111000111")
                    .withNoGuardian()
                    .build();

            assertThatThrownBy(() -> minorWithoutGuardian.handle(MemberUpdateMemberByAdminBuilder.builder()
                    .chipNumber("NEW_CHIP")
                    .build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Guardian is required for minors");
        }
    }

    @Nested
    @DisplayName("Contact information cross-validation")
    class ContactInformationCrossValidation {

        private GuardianInformation aGuardian() {
            return new GuardianInformation("Petr", "Novák", "PARENT",
                    EmailAddress.of("petr@example.com"), PhoneNumber.of("+420987654321"));
        }

        @Test
        @DisplayName("adult member without email at creation should fail with exact message")
        void adultMemberWithoutEmailAtCreationShouldFail() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            PersonalInformation personalInfo = PersonalInformation.of(
                    "Jan", "Novák", LocalDate.of(1990, 5, 15), "CZ", Gender.MALE);

            assertThatThrownBy(() -> Member.register(MemberRegisterMemberBuilder.builder()
                    .id(memberId)
                    .registrationNumber(new RegistrationNumber("ZBM9001"))
                    .personalInformation(personalInfo)
                    .address(Address.of("Ulice 1", "Praha", "11000", "CZ"))
                    .phone(PhoneNumber.of("+420123456789"))
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("At least one email address is required (member or guardian)");
        }

        @Test
        @DisplayName("adult member without phone at creation should fail with exact message")
        void adultMemberWithoutPhoneAtCreationShouldFail() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            PersonalInformation personalInfo = PersonalInformation.of(
                    "Jan", "Novák", LocalDate.of(1990, 5, 15), "CZ", Gender.MALE);

            assertThatThrownBy(() -> Member.register(MemberRegisterMemberBuilder.builder()
                    .id(memberId)
                    .registrationNumber(new RegistrationNumber("ZBM9001"))
                    .personalInformation(personalInfo)
                    .address(Address.of("Ulice 1", "Praha", "11000", "CZ"))
                    .email(EmailAddress.of("jan@example.com"))
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("At least one phone number is required (member or guardian)");
        }

        @Test
        @DisplayName("minor with only guardian email and phone at creation should succeed")
        void minorWithOnlyGuardianContactAtCreationShouldSucceed() {
            LocalDate dateOfBirth = LocalDate.now().minusYears(12);
            int birthYear = dateOfBirth.getYear() % 100;
            MemberId memberId = new MemberId(UUID.randomUUID());
            PersonalInformation personalInfo = PersonalInformation.of(
                    "Anna", "Nováková", dateOfBirth, "CZ", Gender.FEMALE);

            Member member = Member.register(MemberRegisterMemberBuilder.builder()
                    .id(memberId)
                    .registrationNumber(new RegistrationNumber(String.format("ZBM%02d01", birthYear)))
                    .personalInformation(personalInfo)
                    .address(Address.of("Ulice 1", "Praha", "11000", "CZ"))
                    .guardian(aGuardian())
                    .build());

            assertThat(member.getGuardian()).isNotNull();
            assertThat(member.getEmail()).isNull();
            assertThat(member.getPhone()).isNull();
        }

        @Test
        @DisplayName("SelfUpdate that sets new email when guardian exists should succeed")
        void selfUpdateSettingNewEmailWhenGuardianExistsShouldSucceed() {
            Member member = aMember()
                    .withEmail("jan@example.com")
                    .withPhone("+420123456789")
                    .withGuardian(aGuardian())
                    .build();

            member.handle(MemberSelfUpdateBuilder.builder()
                    .email(EmailAddress.of("new@example.com"))
                    .build());

            assertThat(member.getEmail()).isEqualTo(EmailAddress.of("new@example.com"));
        }

        @Test
        @DisplayName("admin update that would leave no email when no guardian should fail")
        void adminUpdateLeavingNoEmailAndNoGuardianShouldFail() {
            EmailAddress noEmail = null;
            Member memberWithNoEmail = aMember()
                    .withEmail(noEmail)
                    .withPhone("+420123456789")
                    .withNoGuardian()
                    .build();

            assertThatThrownBy(() -> memberWithNoEmail.handle(MemberUpdateMemberByAdminBuilder.builder().build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("At least one email address is required (member or guardian)");
        }

        @Test
        @DisplayName("admin update that would leave no phone when no guardian should fail")
        void adminUpdateLeavingNoPhoneAndNoGuardianShouldFail() {
            PhoneNumber noPhone = null;
            Member memberWithNoPhone = aMember()
                    .withEmail("jan@example.com")
                    .withPhone(noPhone)
                    .withNoGuardian()
                    .build();

            assertThatThrownBy(() -> memberWithNoPhone.handle(MemberUpdateMemberByAdminBuilder.builder().build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("At least one phone number is required (member or guardian)");
        }

        @Test
        @DisplayName("member with no email but guardian email should pass update validation")
        void memberWithNoEmailButGuardianEmailShouldPassUpdateValidation() {
            EmailAddress noEmail = null;
            Member member = aMember()
                    .withEmail(noEmail)
                    .withPhone("+420123456789")
                    .withGuardian(aGuardian())
                    .build();

            member.handle(MemberSelfUpdateBuilder.builder().build());

            assertThat(member.getGuardian()).isNotNull();
        }
    }

    @Nested
    @DisplayName("BirthNumber consistency warning tests")
    class BirthNumberConsistencyWarningTests {

        @Test
        @DisplayName("should return no warnings when birth number is null")
        void shouldReturnNoWarningsWhenBirthNumberIsNull() {
            Member member = aMember()
                    .withDateOfBirth(LocalDate.of(1990, 1, 1))
                    .withGender(Gender.MALE)
                    .withNationality("CZE")
                    .withNoGuardian()
                    .build();

            assertThat(member.birthNumberConsistencyWarnings()).isEmpty();
        }

        @Test
        @DisplayName("should return no warnings when birth number matches date of birth and gender")
        void shouldReturnNoWarningsWhenBirthNumberMatchesDateOfBirthAndGender() {
            // 900101/XXXX = year 1990, month 01 (male), day 01 → matches dateOfBirth 1990-01-01, MALE
            Member member = aMember()
                    .withDateOfBirth(LocalDate.of(1990, 1, 1))
                    .withGender(Gender.MALE)
                    .withNationality("CZE")
                    .withBirthNumber("900101/1235")
                    .withNoGuardian()
                    .build();

            assertThat(member.birthNumberConsistencyWarnings()).isEmpty();
        }

        @Test
        @DisplayName("should return date mismatch warning when birth number date does not match dateOfBirth")
        void shouldReturnDateMismatchWarningWhenBirthNumberDateDoesNotMatchDateOfBirth() {
            // 900101/XXXX = 1990-01-01, but member dateOfBirth is 1990-05-15
            Member member = aMember()
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withGender(Gender.MALE)
                    .withNationality("CZE")
                    .withBirthNumber("900101/1235")
                    .withNoGuardian()
                    .build();

            assertThat(member.birthNumberConsistencyWarnings())
                    .hasSize(1)
                    .first().asString().contains("does not match member's date of birth");
        }

        @Test
        @DisplayName("should return gender mismatch warning when birth number indicates different gender")
        void shouldReturnGenderMismatchWarningWhenBirthNumberIndicatesDifferentGender() {
            // 905101/XXXX = month 51 → FEMALE, but member is MALE
            Member member = aMember()
                    .withDateOfBirth(LocalDate.of(1990, 1, 1))
                    .withGender(Gender.MALE)
                    .withNationality("CZE")
                    .withBirthNumber("905101/1239")
                    .withNoGuardian()
                    .build();

            assertThat(member.birthNumberConsistencyWarnings())
                    .hasSize(1)
                    .first().asString().contains("indicates different gender");
        }

        @Test
        @DisplayName("should return both warnings when both date and gender mismatch")
        void shouldReturnBothWarningsWhenBothDateAndGenderMismatch() {
            // 905101/XXXX = 1990-01-01 female, but member is MALE born 1990-05-15
            Member member = aMember()
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withGender(Gender.MALE)
                    .withNationality("CZE")
                    .withBirthNumber("905101/1239")
                    .withNoGuardian()
                    .build();

            assertThat(member.birthNumberConsistencyWarnings()).hasSize(2);
        }

        @Test
        @DisplayName("should return no warnings for female member with female birth number matching date")
        void shouldReturnNoWarningsForFemaleMatchingBirthNumber() {
            // 905115/XXXX = month 51 → FEMALE, day 15, year 1990 → 1990-01-15
            Member member = aMember()
                    .withDateOfBirth(LocalDate.of(1990, 1, 15))
                    .withGender(Gender.FEMALE)
                    .withNationality("CZE")
                    .withBirthNumber("905115/1239")
                    .withNoGuardian()
                    .build();

            assertThat(member.birthNumberConsistencyWarnings()).isEmpty();
        }

        @Test
        @DisplayName("should use two-digit year of dateOfBirth to resolve century when extracting date")
        void shouldUseTwoDigitYearOfDateOfBirthToResolveDate() {
            // 000101/XXXX = year 00, month 01, day 01 → matches 2000-01-01
            Member member = aMember()
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withGender(Gender.MALE)
                    .withNationality("CZE")
                    .withBirthNumber("000101/1235")
                    .withNoGuardian()
                    .build();

            assertThat(member.birthNumberConsistencyWarnings()).isEmpty();
        }

        @Test
        @DisplayName("should return date mismatch warning when birth number year differs from dateOfBirth year")
        void shouldReturnDateMismatchWarningWhenBirthNumberYearDiffers() {
            // 800515/XXXX encodes year 1980, month 05, day 15
            // member dateOfBirth is 1990-05-15 → year mismatch, even though month and day match
            Member member = aMember()
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withGender(Gender.MALE)
                    .withNationality("CZE")
                    .withBirthNumber("800515/1235")
                    .withNoGuardian()
                    .build();

            assertThat(member.birthNumberConsistencyWarnings())
                    .hasSize(1)
                    .first().asString().contains("does not match member's date of birth");
        }
    }
}
