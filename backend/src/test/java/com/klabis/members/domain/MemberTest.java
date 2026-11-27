package com.klabis.members.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.MemberAssert;
import com.klabis.members.BirthNumberAccessedEvent;
import com.klabis.members.MemberCreatedEvent;
import com.klabis.members.MemberSuspendedEvent;
import com.klabis.members.domain.MemberResumeMembershipBuilder;
import com.klabis.members.domain.MemberSuspendMembershipBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.util.Set;
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
 * - Minor contacts are covered by legal guardians (see MemberCompletenessTest)
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
            Address address = new Address("Hlavní 123", "Praha", "11000", Country.of("CZ"));
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    Nationality.of("CZ"),
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
                    .birthNumber(BirthNumber.of("900515/1234"))
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
            assertThat(event.dateOfBirth()).isEqualTo(dateOfBirth);
            assertThat(event.occurredAt()).isNotNull();

        }

        @Test
        @DisplayName("should create minor without own contacts, which are set on the legal guardian group")
        void shouldCreateMinorWithoutContacts() {
            LocalDate dateOfBirth = LocalDate.now().minusYears(10);
            int birthYear = dateOfBirth.getYear() % 100;
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM%02d01".formatted(birthYear));
            Address address = new Address("Školská 456", "Brno", "60200", Country.of("CZ"));
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Anna", "Nováková", dateOfBirth, Nationality.of("CZ"), Gender.FEMALE);

            Member member = Member.register(MemberRegisterMemberBuilder.builder()
                    .id(new MemberId(UUID.randomUUID()))
                    .registrationNumber(registrationNumber)
                    .personalInformation(personalInformation)
                    .address(address)
                    .birthNumber(BirthNumber.of("015315/1234"))
                    .build());

            MemberAssert.assertThat(member).hasRegistrationNumber(registrationNumber);
        }

        @Test
        @DisplayName("should fail when no contact information provided")
        void shouldFailWhenNoContactInformationProvided() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            Address address = new Address("Ulice 1", "Město", "11000", Country.of("CZ"));
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    Nationality.of("CZ"),
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
                    .isInstanceOf(MemberDataRequiredException.class)
                    .hasMessageContaining("At least one email address is required (member or guardian)");
        }

        @Test
        @DisplayName("should fail when email missing but phone provided")
        void shouldFailWhenEmailMissing() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            Address address = new Address("Ulice 1", "Město", "11000", Country.of("CZ"));
            PhoneNumber phone = new PhoneNumber("+420123456789");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    Nationality.of("CZ"),
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
                    .isInstanceOf(MemberDataRequiredException.class)
                    .hasMessageContaining("At least one email address is required (member or guardian)");
        }

        @Test
        @DisplayName("should fail when phone missing but email provided")
        void shouldFailWhenPhoneMissing() {
            // Arrange
            LocalDate dateOfBirth = LocalDate.of(1990, 5, 15);
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            Address address = new Address("Ulice 1", "Město", "11000", Country.of("CZ"));
            EmailAddress email = new EmailAddress("jan@example.com");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    Nationality.of("CZ"),
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
                    .isInstanceOf(MemberDataRequiredException.class)
                    .hasMessageContaining("At least one phone number is required (member or guardian)");
        }

        @Test
        @DisplayName("should fail when first name is blank")
        void shouldFailWhenFirstNameIsBlank() {
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            Address address = new Address("Ulice 1", "Město", "11000", Country.of("CZ"));
            EmailAddress email = new EmailAddress("test@example.com");
            PhoneNumber phone = new PhoneNumber("+420123456789");
            MemberId memberId = new MemberId(UUID.randomUUID());

            assertThatThrownBy(() -> {
                PersonalInformation personalInformation = PersonalInformation.of(
                        "",
                        "Novák",
                        LocalDate.of(1990, 5, 15),
                        Nationality.of("CZ"),
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
            Address address = new Address("Ulice 1", "Město", "11000", Country.of("CZ"));
            EmailAddress email = new EmailAddress("test@example.com");
            PhoneNumber phone = new PhoneNumber("+420123456789");
            MemberId memberId = new MemberId(UUID.randomUUID());

            assertThatThrownBy(() -> {
                PersonalInformation personalInformation = PersonalInformation.of(
                        "Jan",
                        "",
                        LocalDate.of(1990, 5, 15),
                        Nationality.of("CZ"),
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
        @DisplayName("should fail when nationality is missing")
        void shouldFailWhenNationalityIsMissing() {
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9001");
            Address address = new Address("Ulice 1", "Město", "11000", Country.of("CZ"));
            EmailAddress email = new EmailAddress("test@example.com");
            PhoneNumber phone = new PhoneNumber("+420123456789");
            MemberId memberId = new MemberId(UUID.randomUUID());

            assertThatThrownBy(() -> {
                PersonalInformation personalInformation = PersonalInformation.of(
                        "Jan",
                        "Novák",
                        LocalDate.of(1990, 5, 15),
                        null,
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

        @Test
        @DisplayName("should fail when registering CZ member without birth number")
        void shouldFailWhenRegisteringCzMemberWithoutBirthNumber() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            PersonalInformation personalInfo = PersonalInformation.of(
                    "Jan", "Novák", LocalDate.of(1990, 5, 15), Nationality.of("CZ"), Gender.MALE);

            assertThatThrownBy(() -> Member.register(MemberRegisterMemberBuilder.builder()
                    .id(memberId)
                    .registrationNumber(new RegistrationNumber("ZBM9001"))
                    .personalInformation(personalInfo)
                    .address(Address.of("Ulice 1", "Praha", "11000", Country.of("CZ")))
                    .email(EmailAddress.of("jan@example.com"))
                    .phone(PhoneNumber.of("+420123456789"))
                    .build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Birth number is required for Czech nationals");
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
                    .withNationality("SK")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", Country.of("CZ")))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
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
            Member.SuspendMembership command = MemberSuspendMembershipBuilder.builder()
                    .suspendedBy(adminUserId)
                    .reason(DeactivationReason.ODHLASKA)
                    .note("Member requested termination")
                    .build();

            // Act
            activeMember.suspend(command);

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
            Member.SuspendMembership command = MemberSuspendMembershipBuilder.builder()
                    .suspendedBy(adminUserId)
                    .reason(DeactivationReason.PRESTUP)
                    .note(null)
                    .build();

            // Act
            // Clear creation event first, then suspend
            activeMember.clearDomainEvents();  // Clear the MemberCreatedEvent from registration
            activeMember.suspend(command);

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
            Member.SuspendMembership firstCommand = MemberSuspendMembershipBuilder.builder()
                    .suspendedBy(adminUserId)
                    .reason(DeactivationReason.ODHLASKA)
                    .note("First termination")
                    .build();
            activeMember.suspend(firstCommand);

            UserId anotherAdmin = new UserId(UUID.randomUUID());
            Member.SuspendMembership secondCommand = MemberSuspendMembershipBuilder.builder()
                    .suspendedBy(anotherAdmin)
                    .reason(DeactivationReason.OTHER)
                    .note("Second termination attempt")
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> activeMember.suspend(secondCommand))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("already suspended");
        }

        @Test
        @DisplayName("should reject suspension without a reason and keep member active")
        void shouldRejectSuspensionWithoutReason() {
            Member activeMember = createActiveMember();
            Member.SuspendMembership command = new Member.SuspendMembership(new UserId(UUID.randomUUID()), null, "note");

            assertThatThrownBy(() -> activeMember.suspend(command))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("reason");
            assertThat(activeMember.isActive()).isTrue();
        }

        @Test
        @DisplayName("should allow suspension with all reason types")
        void shouldAllowSuspensionWithAllReasonTypes() {
            // Arrange
            UserId adminUserId = new UserId(UUID.randomUUID());

            // Test ODHLASKA
            Member member1 = createActiveMember();
            member1.suspend(MemberSuspendMembershipBuilder.builder()
                    .suspendedBy(adminUserId).reason(DeactivationReason.ODHLASKA).note("Note 1").build());
            assertThat(member1.getSuspensionReason()).isEqualTo(DeactivationReason.ODHLASKA);

            // Test PRESTUP
            Member member2 = createActiveMember();
            member2.suspend(MemberSuspendMembershipBuilder.builder()
                    .suspendedBy(adminUserId).reason(DeactivationReason.PRESTUP).note("Note 2").build());
            assertThat(member2.getSuspensionReason()).isEqualTo(DeactivationReason.PRESTUP);

            // Test OTHER
            Member member3 = createActiveMember();
            member3.suspend(MemberSuspendMembershipBuilder.builder()
                    .suspendedBy(adminUserId).reason(DeactivationReason.OTHER).note("Note 3").build());
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
            activeMember.suspend(MemberSuspendMembershipBuilder.builder()
                    .suspendedBy(adminUserId).reason(DeactivationReason.ODHLASKA).note(null).build());
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
                    .withNationality("SK")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", Country.of("CZ")))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .build();
        }

        private Member createSuspendedMember() {
            Member activeMember = createActiveMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            Member.SuspendMembership suspendCommand = MemberSuspendMembershipBuilder.builder()
                    .suspendedBy(adminUserId)
                    .reason(DeactivationReason.ODHLASKA)
                    .note("Member requested termination")
                    .build();
            activeMember.suspend(suspendCommand);
            return activeMember;
        }

        @Test
        @DisplayName("should resume suspended member")
        void shouldResumeSuspendedMember() {
            // Arrange
            Member suspendedMember = createSuspendedMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            Member.ResumeMembership command = MemberResumeMembershipBuilder.builder()
                    .resumedBy(adminUserId).build();

            // Act
            suspendedMember.resume(command);

            // Assert
            assertThat(suspendedMember.isActive()).isTrue();
            assertThat(suspendedMember.getSuspensionReason()).isNull();
            assertThat(suspendedMember.getSuspendedAt()).isNull();
            assertThat(suspendedMember.getSuspensionNote()).isNull();
            assertThat(suspendedMember.getSuspendedBy()).isNull();
        }

        @Test
        @DisplayName("should reject resume of already active member")
        void shouldRejectResumeOfAlreadyActiveMember() {
            // Arrange
            Member activeMember = createActiveMember();
            UserId adminUserId = new UserId(UUID.randomUUID());
            Member.ResumeMembership command = MemberResumeMembershipBuilder.builder()
                    .resumedBy(adminUserId).build();

            // Act & Assert
            assertThatThrownBy(() -> activeMember.resume(command))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("already active");
        }
    }

    @Nested
    @DisplayName("handle(UpdateMember) command")
    class HandleUpdateMember {

        private Member createAdultMember() {
            return aMember()
                    .withRegistrationNumber("ZBM9001")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", Country.of("CZ")))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .withBirthNumber("900515/1234")
                    .build();
        }

        @Test
        @DisplayName("updates owner-editable fields, clears a field set to null and leaves absent fields and personal information untouched")
        void shouldUpdateOwnerEditableFields() {
            Member member = createAdultMember();
            EmailAddress newEmail = EmailAddress.of("new@example.com");
            PhoneNumber newPhone = PhoneNumber.of("+420999888777");
            Address newAddress = Address.of("Nová 1", "Brno", "60200", Country.of("CZ"));

            update(member, MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(member))
                    .email(newEmail).phone(newPhone).address(newAddress).dietaryRestrictions("Vegan").build());

            assertThat(member.getEmail()).isEqualTo(newEmail);
            assertThat(member.getPhone()).isEqualTo(newPhone);
            assertThat(member.getAddress()).isEqualTo(newAddress);
            assertThat(member.getDietaryRestrictions()).isEqualTo("Vegan");
            assertThat(member.getFirstName()).isEqualTo("Jan");
            assertThat(member.getLastName()).isEqualTo("Novák");
            assertThat(member.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
            assertThat(member.getGender()).isEqualTo(Gender.MALE);

            update(member, MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(member)).chipNumber("999").build());
            assertThat(member.getDietaryRestrictions()).isEqualTo("Vegan");

            update(member, MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(member))
                    .dietaryRestrictions(null).build());
            assertThat(member.getDietaryRestrictions()).isNull();
        }

        @Test
        @DisplayName("updates admin-only personal information and leaves contact details untouched")
        void shouldUpdateAdminOnlyFields() {
            Member member = createAdultMember();
            LocalDate newDob = LocalDate.of(1985, 3, 20);

            update(member, MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(member))
                    .firstName("Petr").lastName("Svoboda").dateOfBirth(newDob).gender(Gender.FEMALE).build());

            assertThat(member.getFirstName()).isEqualTo("Petr");
            assertThat(member.getLastName()).isEqualTo("Svoboda");
            assertThat(member.getDateOfBirth()).isEqualTo(newDob);
            assertThat(member.getGender()).isEqualTo(Gender.FEMALE);
            assertThat(member.getEmail().value()).isEqualTo("jan.novak@example.com");
            assertThat(member.getPhone().value()).isEqualTo("+420123456789");
            assertThat(member.getAddress().street()).isEqualTo("Hlavní 123");
        }

        @Test
        @DisplayName("should allow dateOfBirth update that makes member a minor and mark the data incomplete")
        void shouldAllowDateOfBirthUpdateThatMakesMemberMinor() {
            Member member = createAdultMember();
            LocalDate minorDateOfBirth = LocalDate.now().minusYears(10);

            update(member, MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(member))
                    .dateOfBirth(minorDateOfBirth)
                    .build());

            assertThat(member.getDateOfBirth()).isEqualTo(minorDateOfBirth);
            assertThat(member.isDataIncomplete()).isTrue();
        }

        @Test
        @DisplayName("should allow clearing the email of a minor whose legal guardians have contacts")
        void shouldAllowClearingEmailOfMinorCoveredByGuardians() {
            Member minor = aMember()
                    .withDateOfBirth(LocalDate.now().minusYears(10))
                    .withNationality("SK")
                    .withEmail("anna@example.com")
                    .withPhone("+420123456789")
                    .build();

            minor.update(MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(minor)).email(null).build(),
                    new GuardianContacts(true, true, true));

            assertThat(minor.getEmail()).isNull();
            assertThat(minor.isDataIncomplete()).isFalse();
        }

        @Test
        @DisplayName("should reject clearing the email of a minor without a guardian with contacts")
        void shouldRejectClearingEmailOfMinorWithoutGuardianCoverage() {
            Member minor = aMember()
                    .withDateOfBirth(LocalDate.now().minusYears(10))
                    .withNationality("SK")
                    .withEmail("anna@example.com")
                    .withPhone("+420123456789")
                    .build();

            assertThatThrownBy(() -> minor.update(
                    MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(minor)).email(null).build(),
                    new GuardianContacts(true, false, true)))
                    .isInstanceOf(MemberDataRequiredException.class)
                    .hasMessageContaining("At least one email address is required");
        }

        @Test
        @DisplayName("should clear birth number when set on non-Czech member")
        void shouldClearBirthNumberWhenSetOnNonCzechMember() {
            Member member = aMember()
                    .withRegistrationNumber("ZBM9001")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withNationality("SK")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Bratislava", "81102", Country.of("SK")))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .build();

            update(member, MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(member))
                    .birthNumber(BirthNumber.of("9005151234"))
                    .build());

            assertThat(member.getBirthNumber()).isNull();
        }

        @Test
        @DisplayName("should allow setting birth number on Czech member")
        void shouldAllowSettingBirthNumberOnCzechMember() {
            Member member = createAdultMember();

            update(member, MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(member))
                    .birthNumber(BirthNumber.of("9005151234"))
                    .build());

            assertThat(member.getBirthNumber()).isNotNull();
        }

        @Test
        @DisplayName("should fail when updating member nationality to CZ without providing a birth number")
        void shouldFailWhenUpdatingNationalityToCzWithoutBirthNumber() {
            Member member = aMember()
                    .withRegistrationNumber("ZBM9001")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withNationality("SK")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Bratislava", "81102", Country.of("SK")))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .build();

            assertThatThrownBy(() -> update(member, MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(member))
                    .nationality(Nationality.of("CZ"))
                    .build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Birth number is required for Czech nationals");
        }

        @Test
        @DisplayName("should allow unrelated update on existing minor already missing guardian (never-worsen)")
        void shouldAllowUnrelatedUpdateOnExistingMinorAlreadyMissingGuardian() {
            Member minorWithoutGuardian = aMember()
                    .withRegistrationNumber("ZBM1001")
                    .withName("Jana", "Malá")
                    .withDateOfBirth(LocalDate.now().minusYears(10))
                    .withNationality("CZ")
                    .withGender(Gender.FEMALE)
                    .withAddress(Address.of("Dětská 1", "Praha", "11000", Country.of("CZ")))
                    .withEmail("guardian@example.com")
                    .withPhone("+420111000111")
                    .withBirthNumber("015315/1234")
                    .build();

            update(minorWithoutGuardian, MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(minorWithoutGuardian))
                    .chipNumber("NEW_CHIP")
                    .build());

            assertThat(minorWithoutGuardian.getChipNumber()).isEqualTo("NEW_CHIP");
            assertThat(missing(minorWithoutGuardian)).containsExactly(MissingDataItem.GUARDIAN);
        }
    }

    @Nested
    @DisplayName("Contact information cross-validation")
    class ContactInformationCrossValidation {

        private Member completeMember() {
            return aMember()
                    .withEmail("jan@example.com")
                    .withPhone("+420123456789")
                    .build();
        }

        private Member memberLacking(MissingDataItem item) {
            return aMember()
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withEmail(item == MissingDataItem.EMAIL ? null : EmailAddress.of("jan@example.com"))
                    .withPhone(item == MissingDataItem.PHONE ? null : PhoneNumber.of("+420123456789"))
                    .withAddress(item == MissingDataItem.ADDRESS ? null
                            : Address.of("Hlavní 123", "Praha", "11000", Country.of("CZ")))
                    .build();
        }

        @Test
        @DisplayName("adult member without email at creation should fail with exact message")
        void adultMemberWithoutEmailAtCreationShouldFail() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            PersonalInformation personalInfo = PersonalInformation.of(
                    "Jan", "Novák", LocalDate.of(1990, 5, 15), Nationality.of("CZ"), Gender.MALE);

            assertThatThrownBy(() -> Member.register(MemberRegisterMemberBuilder.builder()
                    .id(memberId)
                    .registrationNumber(new RegistrationNumber("ZBM9001"))
                    .personalInformation(personalInfo)
                    .address(Address.of("Ulice 1", "Praha", "11000", Country.of("CZ")))
                    .phone(PhoneNumber.of("+420123456789"))
                    .build()))
                    .isInstanceOf(MemberDataRequiredException.class)
                    .hasMessage("At least one email address is required (member or guardian)");
        }

        @Test
        @DisplayName("adult member without phone at creation should fail with exact message")
        void adultMemberWithoutPhoneAtCreationShouldFail() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            PersonalInformation personalInfo = PersonalInformation.of(
                    "Jan", "Novák", LocalDate.of(1990, 5, 15), Nationality.of("CZ"), Gender.MALE);

            assertThatThrownBy(() -> Member.register(MemberRegisterMemberBuilder.builder()
                    .id(memberId)
                    .registrationNumber(new RegistrationNumber("ZBM9001"))
                    .personalInformation(personalInfo)
                    .address(Address.of("Ulice 1", "Praha", "11000", Country.of("CZ")))
                    .email(EmailAddress.of("jan@example.com"))
                    .build()))
                    .isInstanceOf(MemberDataRequiredException.class)
                    .hasMessage("At least one phone number is required (member or guardian)");
        }

        @ParameterizedTest(name = "update that clears {0} on a complete member should fail")
        @CsvSource({
                "EMAIL, At least one email address is required (member or guardian)",
                "PHONE, At least one phone number is required (member or guardian)",
                "ADDRESS, Address is required"
        })
        void updateClearingItemOnCompleteMemberShouldFail(MissingDataItem item, String message) {
            Member member = completeMember();
            assertThat(missing(member)).isEmpty();
            var clearing = MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(member));
            switch (item) {
                case EMAIL -> clearing.email(null);
                case PHONE -> clearing.phone(null);
                case ADDRESS -> clearing.address(null);
                default -> throw new IllegalArgumentException(item.name());
            }

            assertThatThrownBy(() -> update(member, clearing.build()))
                    .isInstanceOf(MemberDataRequiredException.class)
                    .hasMessage(message);
        }

        @ParameterizedTest(name = "unrelated update on member already missing {0} should succeed (never-worsen)")
        @EnumSource(value = MissingDataItem.class, names = {"ADDRESS", "EMAIL", "PHONE"})
        void unrelatedUpdateOnIncompleteMemberShouldSucceed(MissingDataItem item) {
            Member member = memberLacking(item);

            update(member, MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(member))
                    .dietaryRestrictions("Vegan")
                    .build());

            assertThat(member.getDietaryRestrictions()).isEqualTo("Vegan");
            assertThat(missing(member)).containsExactly(item);
        }

        @Test
        @DisplayName("incomplete member can fill in the missing item it was lacking")
        void incompleteMemberCanFillInMissingEmail() {
            Member memberWithNoEmail = memberLacking(MissingDataItem.EMAIL);

            update(memberWithNoEmail, MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(memberWithNoEmail))
                    .email(EmailAddress.of("filled@example.com"))
                    .build());

            assertThat(memberWithNoEmail.getEmail()).isEqualTo(EmailAddress.of("filled@example.com"));
            assertThat(missing(memberWithNoEmail).isEmpty()).isTrue();
        }

        @Test
        @DisplayName("incomplete member cannot lose an additional item")
        void incompleteMemberCannotLoseAdditionalItem() {
            Member memberWithNoEmail = memberLacking(MissingDataItem.EMAIL);
            assertThat(missing(memberWithNoEmail)).containsExactly(MissingDataItem.EMAIL);

            assertThatThrownBy(() -> update(memberWithNoEmail,
                    MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(memberWithNoEmail)).phone(null).build()))
                    .isInstanceOf(MemberDataRequiredException.class)
                    .hasMessage("At least one phone number is required (member or guardian)");
        }
    }

    @Nested
    @DisplayName("handle(SyncFromOris) command")
    class HandleSyncFromOris {

        private Member createAdultMemberWithAllOptionalFieldsSet() {
            Member member = aMember()
                    .withRegistrationNumber("ZBM9001")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(1990, 5, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", Country.of("CZ")))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420123456789")
                    .withBirthNumber("900515/1234")
                    .build();

            update(member, MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(member))
                    .chipNumber("999")
                    .identityCard(IdentityCard.of("123456789", LocalDate.of(2030, 1, 1)))
                    .drivingLicenseGroup(DrivingLicenseGroup.B)
                    .medicalCourse(MedicalCourse.of(LocalDate.of(2020, 1, 1), java.util.Optional.of(LocalDate.of(2030, 1, 1))))
                    .trainerLicense(TrainerLicense.of(TrainerLevel.T1, LocalDate.of(2030, 1, 1)))
                    .refereeLicense(RefereeLicense.of(RefereeLevel.R1, LocalDate.of(2030, 1, 1)))
                    .dietaryRestrictions("Vegan")
                    .bankAccountNumber(BankAccountNumber.of("12345/5678"))
                    .build());

            UserId adminUserId = new UserId(UUID.randomUUID());
            member.suspend(MemberSuspendMembershipBuilder.builder()
                    .suspendedBy(adminUserId)
                    .reason(DeactivationReason.ODHLASKA)
                    .note("Some note")
                    .build());

            return member;
        }

        private Member.SyncFromOris syncCommandFor(Member member) {
            return MemberSyncFromOrisBuilder.builder()
                    .registrationNumber(member.getRegistrationNumber())
                    .firstName("Petr")
                    .lastName("Svoboda")
                    .dateOfBirth(LocalDate.of(1985, 3, 20))
                    .gender(Gender.FEMALE)
                    .nationality(Nationality.of("SK"))
                    .birthNumber(null)
                    .email(EmailAddress.of("petr.svoboda@example.com"))
                    .phone(PhoneNumber.of("+420999888777"))
                    .address(Address.of("Nová 1", "Brno", "60200", Country.of("CZ")))
                    .chipNumber("111")
                    .build();
        }

        @Test
        @DisplayName("should update every ORIS-owned field")
        void shouldUpdateEveryOrisOwnedField() {
            Member member = createAdultMemberWithAllOptionalFieldsSet();
            Member.SyncFromOris command = syncCommandFor(member);

            member.syncFromOris(command);

            assertThat(member.getFirstName()).isEqualTo("Petr");
            assertThat(member.getLastName()).isEqualTo("Svoboda");
            assertThat(member.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 3, 20));
            assertThat(member.getGender()).isEqualTo(Gender.FEMALE);
            assertThat(member.getNationality().code()).isEqualTo("SK");
            assertThat(member.getBirthNumber()).isNull();
            assertThat(member.getEmail()).isEqualTo(EmailAddress.of("petr.svoboda@example.com"));
            assertThat(member.getPhone()).isEqualTo(PhoneNumber.of("+420999888777"));
            assertThat(member.getAddress()).isEqualTo(Address.of("Nová 1", "Brno", "60200", Country.of("CZ")));
            assertThat(member.getChipNumber()).isEqualTo("111");
        }

        @Test
        @DisplayName("should leave Klabis-owned fields untouched")
        void shouldLeaveKlabisOwnedFieldsUntouched() {
            Member member = createAdultMemberWithAllOptionalFieldsSet();
            Member.SyncFromOris command = syncCommandFor(member);

            member.syncFromOris(command);

            assertThat(member.getIdentityCard()).isEqualTo(IdentityCard.of("123456789", LocalDate.of(2030, 1, 1)));
            assertThat(member.getDrivingLicenseGroup()).isEqualTo(DrivingLicenseGroup.B);
            assertThat(member.getMedicalCourse()).isEqualTo(MedicalCourse.of(LocalDate.of(2020, 1, 1), java.util.Optional.of(LocalDate.of(2030, 1, 1))));
            assertThat(member.getTrainerLicense()).isEqualTo(TrainerLicense.of(TrainerLevel.T1, LocalDate.of(2030, 1, 1)));
            assertThat(member.getRefereeLicense()).isEqualTo(RefereeLicense.of(RefereeLevel.R1, LocalDate.of(2030, 1, 1)));
            assertThat(member.getDietaryRestrictions()).isEqualTo("Vegan");
            assertThat(member.getBankAccountNumber()).isEqualTo(BankAccountNumber.of("12345/5678"));

            assertThat(member.isActive()).isFalse();
            assertThat(member.getSuspensionReason()).isEqualTo(DeactivationReason.ODHLASKA);
            assertThat(member.getSuspensionNote()).isEqualTo("Some note");
            assertThat(member.getSuspendedAt()).isNotNull();
            assertThat(member.getSuspendedBy()).isNotNull();
        }

        @Test
        @DisplayName("should not publish BirthNumberAccessedEvent even when birth number changes")
        void shouldNotPublishBirthNumberAccessedEventEvenWhenBirthNumberChanges() {
            Member member = createAdultMemberWithAllOptionalFieldsSet();
            member.clearDomainEvents();

            Member.SyncFromOris command = MemberSyncFromOrisBuilder.builder()
                    .registrationNumber(member.getRegistrationNumber())
                    .firstName(member.getFirstName())
                    .lastName(member.getLastName())
                    .dateOfBirth(member.getDateOfBirth())
                    .gender(member.getGender())
                    .nationality(Nationality.of("CZ"))
                    .birthNumber(BirthNumber.of("850320/1234"))
                    .email(member.getEmail())
                    .phone(member.getPhone())
                    .address(member.getAddress())
                    .chipNumber(member.getChipNumber())
                    .build();

            member.syncFromOris(command);

            assertThat(member.getBirthNumber()).isEqualTo(BirthNumber.of("850320/1234"));
            assertThat(member.getDomainEvents())
                    .noneMatch(event -> event instanceof BirthNumberAccessedEvent);
        }

        @Test
        @DisplayName("accepts ORIS removing the member's only phone or birth number, leaving them incomplete")
        void acceptsLosingPhoneOrBirthNumberAndBecomesIncomplete() {
            Member member = createAdultMemberWithAllOptionalFieldsSet();

            Member.SyncFromOris command = MemberSyncFromOrisBuilder.builder()
                    .registrationNumber(member.getRegistrationNumber())
                    .firstName(member.getFirstName())
                    .lastName(member.getLastName())
                    .dateOfBirth(member.getDateOfBirth())
                    .gender(member.getGender())
                    .nationality(Nationality.of("CZ"))
                    .birthNumber(null)
                    .email(member.getEmail())
                    .phone(null)
                    .address(member.getAddress())
                    .chipNumber(member.getChipNumber())
                    .build();

            member.syncFromOris(command);

            assertThat(member.getPhone()).isNull();
            assertThat(member.getBirthNumber()).isNull();
            assertThat(missing(member)).containsExactlyInAnyOrder(
                    MissingDataItem.PHONE, MissingDataItem.BIRTH_NUMBER);
            assertThat(missing(member).isEmpty()).isFalse();
        }

        @Test
        @DisplayName("still refuses a birth number for a non-Czech national (consistency rule)")
        void stillRefusesBirthNumberForNonCzechNational() {
            Member member = createAdultMemberWithAllOptionalFieldsSet();

            Member.SyncFromOris command = MemberSyncFromOrisBuilder.builder()
                    .registrationNumber(member.getRegistrationNumber())
                    .firstName(member.getFirstName())
                    .lastName(member.getLastName())
                    .dateOfBirth(member.getDateOfBirth())
                    .gender(member.getGender())
                    .nationality(Nationality.of("SK"))
                    .birthNumber(BirthNumber.of("850320/1234"))
                    .email(member.getEmail())
                    .phone(member.getPhone())
                    .address(member.getAddress())
                    .chipNumber(member.getChipNumber())
                    .build();

            assertThatThrownBy(() -> member.syncFromOris(command))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Birth number is only allowed for Czech nationals");
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
                    .withNationality("CZ")
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
                    .withNationality("CZ")
                    .withBirthNumber("900101/1235")
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
                    .withNationality("CZ")
                    .withBirthNumber("900101/1235")
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
                    .withNationality("CZ")
                    .withBirthNumber("905101/1239")
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
                    .withNationality("CZ")
                    .withBirthNumber("905101/1239")
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
                    .withNationality("CZ")
                    .withBirthNumber("905115/1239")
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
                    .withNationality("CZ")
                    .withBirthNumber("000101/1235")
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
                    .withNationality("CZ")
                    .withBirthNumber("800515/1235")
                    .build();

            assertThat(member.birthNumberConsistencyWarnings())
                    .hasSize(1)
                    .first().asString().contains("does not match member's date of birth");
        }
    }

    private static void update(Member member, Member.UpdateMember command) {
        member.update(command, GuardianContacts.NONE);
    }

    private static Set<MissingDataItem> missing(Member member) {
        return MemberCompleteness.missingData(member, GuardianContacts.NONE);
    }
}
