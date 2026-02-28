package com.klabis.members.application;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.MemberAssert;
import com.klabis.members.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RegistrationServiceImpl}.
 * <p>
 * Tests cover the member registration functionality including:
 * <ul>
 *   <li>User account creation with pending activation status</li>
 *   <li>Guardian information handling</li>
 *   <li>Transactional integrity of member and user creation</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RegistrationService Unit Tests")
class RegistrationServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UserService userService;

    @Mock
    private RegistrationNumberGenerator registrationNumberGenerator;

    private RegistrationService service;

    @BeforeEach
    void setUp() {
        service = new RegistrationServiceImpl(
                memberRepository,
                userService,
                registrationNumberGenerator
        );

        // Setup default mock behavior that can be overridden in individual tests
        // Use a fixed shared ID for all tests by default
        UserId defaultSharedId = new UserId(UUID.fromString("12345678-1234-1234-1234-123456789012"));
        mockUserCreation(defaultSharedId);
        mockMemberCreation(defaultSharedId);

        // Setup default registration number generator
        when(registrationNumberGenerator.generate(any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate date = invocation.getArgument(0);
                    // Return different registration numbers based on date for testing
                    if (date.equals(LocalDate.of(2005, 3, 20))) {
                        return new RegistrationNumber("ZBM0501");
                    } else if (date.equals(LocalDate.of(2005, 7, 20))) {
                        return new RegistrationNumber("ZBM0502");
                    } else {
                        return new RegistrationNumber("ZBM0500");
                    }
                });
    }

    /**
     * Helper method to configure mock behavior for member creation.
     * <p>
     * <b>Critical:</b> Must use the same shared ID as User mock to satisfy
     * the invariant: Member ID must equal User ID.
     *
     * @param sharedId the shared ID to use for both User and Member
     */
    private void mockMemberCreation(UserId sharedId) {
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            // Return the member as-is (it already has the shared ID from User)
            return member;
        });
    }

    /**
     * Helper method to configure mock behavior for user creation.
     * Can be called from individual tests to override defaults.
     * <p>
     * <b>Critical:</b> UserService must return the same shared ID as Member mock
     * to satisfy the invariant: Member ID must equal User ID.
     *
     * @param hashedPassword the hashed password to return
     * @param sharedId       the shared ID to use for both User and Member
     */
    private void mockUserCreation(UserId sharedId) {
        when(userService.createUser(anyString(), anyString(), any(Set.class))).thenReturn(sharedId);
    }

    @Nested
    @DisplayName("registerMember() method")
    class RegisterMemberMethod {

        @Test
        @DisplayName("should create member with provided registration number")
        void shouldCreateMemberWithProvidedRegistrationNumber() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2005, 6, 15);
            UserId userId = new UserId(UUID.fromString("12345678-1234-1234-1234-123456789012"));
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0500");
            Address address = Address.of("Hlavní 123", "Praha", "11000", "CZ");
            EmailAddress email = EmailAddress.of("jan.novak@example.com");
            PhoneNumber phone = PhoneNumber.of("+420777888999");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan", "Novák", dateOfBirth, "CZ", Gender.MALE
            );

            RegistrationService.RegisterNewMember command = new RegistrationService.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    null,
                    null,
                    null
            );

            // When
            Member result = service.registerMember(command);

            // Then - verify that a Member is returned
            assertThat(result).isNotNull();

            // Verify member repository interactions
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            MemberAssert.assertThat(savedMember)
                    .hasFirstName("Jan")
                    .hasLastName("Novák")
                    .hasDateOfBirth(dateOfBirth)
                    .hasGender(Gender.MALE);
            assertThat(savedMember.getRegistrationNumber().getValue()).isEqualTo("ZBM0500");

            // Verify user creation via UserService with correct arguments
            ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Set> authoritiesCaptor = ArgumentCaptor.forClass(Set.class);
            verify(userService).createUser(usernameCaptor.capture(), emailCaptor.capture(), authoritiesCaptor.capture());

            assertThat(usernameCaptor.getValue()).isEqualTo("ZBM0500");
            assertThat(emailCaptor.getValue()).isEqualTo("jan.novak@example.com");
            assertThat(authoritiesCaptor.getValue()).isEqualTo(Set.of(Authority.MEMBERS_READ, Authority.EVENTS_READ));

            // CRITICAL: Verify returned Member has the shared ID
            assertThat(result.getId().toUserId().uuid()).isEqualTo(userId.uuid());
        }

        @Test
        @DisplayName("should create user account with provided registration number")
        void shouldCreateUserAccountWithProvidedRegistrationNumber() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2005, 3, 20);
            UserId testSharedId = new UserId(UUID.fromString("87654321-4321-4321-4321-210987654321"));
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0501");
            Address address = Address.of("Štúrova 45", "Bratislava", "81101", "SK");
            EmailAddress email = EmailAddress.of("eva@example.com");
            PhoneNumber phone = PhoneNumber.of("+421777888999");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Eva", "Svobodová", dateOfBirth, "SK", Gender.FEMALE
            );

            RegistrationService.RegisterNewMember command = new RegistrationService.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    null,
                    null,
                    null
            );

            mockUserCreation(testSharedId);
            mockMemberCreation(testSharedId);

            // When
            service.registerMember(command);

            // Then
            ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Set> authoritiesCaptor = ArgumentCaptor.forClass(Set.class);
            verify(userService).createUser(usernameCaptor.capture(), emailCaptor.capture(), authoritiesCaptor.capture());

            assertThat(usernameCaptor.getValue()).isEqualTo("ZBM0501");
            assertThat(emailCaptor.getValue()).isEqualTo("eva@example.com");
            assertThat(authoritiesCaptor.getValue()).isEqualTo(Set.of(Authority.MEMBERS_READ, Authority.EVENTS_READ));
        }

        @Test
        @DisplayName("should handle member with guardian information")
        void shouldHandleMemberWithGuardian() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2010, 1, 15);
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM1000");
            Address address = Address.of("Dětská 1", "Brno", "60200", "CZ");
            EmailAddress email = EmailAddress.of("child@example.com");
            PhoneNumber phone = PhoneNumber.of("+420777333444");
            EmailAddress guardianEmail = EmailAddress.of("parent@example.com");
            PhoneNumber guardianPhone = PhoneNumber.of("+420777111222");
            GuardianInformation guardian = new GuardianInformation(
                    "Parent", "Name", "PARENT", guardianEmail, guardianPhone
            );
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Child", "Minor", dateOfBirth, "CZ", Gender.MALE
            );

            RegistrationService.RegisterNewMember command = new RegistrationService.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    guardian,
                    null,
                    null
            );

            mockUserCreation(testSharedId);
            mockMemberCreation(testSharedId);

            // When
            service.registerMember(command);

            // Then
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            MemberAssert.assertThat(savedMember).hasGuardianNotNull();
            assertThat(savedMember.getGuardian().getFirstName()).isEqualTo("Parent");
            assertThat(savedMember.getGuardian().getLastName()).isEqualTo("Name");
            assertThat(savedMember.getGuardian().getRelationship()).isEqualTo("PARENT");
        }

        @Test
        @DisplayName("should handle member without guardian")
        void shouldHandleMemberWithoutGuardian() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(1990, 5, 10);
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM9005");
            Address address = Address.of("Náměstí Svobody 1", "Ostrava", "70200", "CZ");
            EmailAddress email = EmailAddress.of("adult@example.com");
            PhoneNumber phone = PhoneNumber.of("+420777555666");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Adult", "Member", dateOfBirth, "CZ", Gender.FEMALE
            );

            RegistrationService.RegisterNewMember command = new RegistrationService.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    null,
                    null,
                    null
            );

            mockUserCreation(testSharedId);
            mockMemberCreation(testSharedId);

            // When
            service.registerMember(command);

            // Then
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getGuardian()).isNull();
        }

        @Test
        @DisplayName("should create member with provided registration number for different year")
        void shouldCreateMemberWithProvidedRegistrationNumberForDifferentYear() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2005, 7, 20);
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0502");
            Address address = Address.of("Testovací 5", "Plzeň", "30100", "CZ");
            EmailAddress email = EmailAddress.of("test@example.com");
            PhoneNumber phone = PhoneNumber.of("+420777777777");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Test", "Member", dateOfBirth, "CZ", Gender.MALE
            );

            RegistrationService.RegisterNewMember command = new RegistrationService.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    null,
                    null,
                    null
            );

            mockUserCreation(testSharedId);
            mockMemberCreation(testSharedId);

            // When
            service.registerMember(command);

            // Then
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getRegistrationNumber().getValue()).isEqualTo("ZBM0502");
        }

        @Test
        @DisplayName("should create user and member in same transaction")
        void shouldCreateUserAndMemberInSameTransaction() {
            // Given
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0000");
            Address address = Address.of("Transakční 10", "Liberec", "46001", "CZ");
            EmailAddress email = EmailAddress.of("transaction@example.com");
            PhoneNumber phone = PhoneNumber.of("+420777000000");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Transaction", "Test", LocalDate.of(2000, 1, 1), "CZ", Gender.MALE
            );

            RegistrationService.RegisterNewMember command = new RegistrationService.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    null,
                    null,
                    null
            );

            mockUserCreation(testSharedId);
            mockMemberCreation(testSharedId);

            // When
            service.registerMember(command);

            // Then - verify member repository and user service were called
            verify(memberRepository).save(any(Member.class));
            verify(userService).createUser(anyString(), anyString(), any(Set.class));
        }

        @Test
        @DisplayName("should register member with birth number for Czech nationality")
        void shouldRegisterMemberWithBirthNumberForCzechNationality() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2005, 6, 15);
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0500");
            Address address = Address.of("Hlavní 123", "Praha", "11000", "CZ");
            EmailAddress email = EmailAddress.of("jan.novak@example.com");
            PhoneNumber phone = PhoneNumber.of("+420777888999");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan", "Novák", dateOfBirth, "CZ", Gender.MALE
            );
            BirthNumber birthNumber = BirthNumber.of("900101/1234");

            RegistrationService.RegisterNewMember command = new RegistrationService.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    null,
                    birthNumber,
                    null
            );

            mockUserCreation(testSharedId);
            mockMemberCreation(testSharedId);

            // When
            Member result = service.registerMember(command);

            // Then
            assertThat(result.getId().toUserId().uuid()).isEqualTo(testSharedId.uuid());

            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getBirthNumber()).isNotNull();
            assertThat(savedMember.getBirthNumber().value()).isEqualTo("900101/1234");
        }

        @Test
        @DisplayName("should register member with bank account number")
        void shouldRegisterMemberWithBankAccountNumber() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2005, 6, 15);
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0500");
            Address address = Address.of("Hlavní 123", "Praha", "11000", "CZ");
            EmailAddress email = EmailAddress.of("jan.novak@example.com");
            PhoneNumber phone = PhoneNumber.of("+420777888999");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan", "Novák", dateOfBirth, "CZ", Gender.MALE
            );
            BankAccountNumber bankAccountNumber = BankAccountNumber.of("CZ6508000000192000145399");

            RegistrationService.RegisterNewMember command = new RegistrationService.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    null,
                    null,
                    bankAccountNumber
            );

            mockUserCreation(testSharedId);
            mockMemberCreation(testSharedId);

            // When
            Member result = service.registerMember(command);

            // Then
            assertThat(result.getId().toUserId().uuid()).isEqualTo(testSharedId.uuid());

            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getBankAccountNumber()).isNotNull();
            assertThat(savedMember.getBankAccountNumber().value()).isEqualTo("CZ6508000000192000145399");
        }

        @Test
        @DisplayName("should register member with both birth number and bank account")
        void shouldRegisterMemberWithBothBirthNumberAndBankAccount() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2005, 6, 15);
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0500");
            Address address = Address.of("Hlavní 123", "Praha", "11000", "CZ");
            EmailAddress email = EmailAddress.of("jan.novak@example.com");
            PhoneNumber phone = PhoneNumber.of("+420777888999");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan", "Novák", dateOfBirth, "CZ", Gender.MALE
            );
            BirthNumber birthNumber = BirthNumber.of("900101/1234");
            BankAccountNumber bankAccountNumber = BankAccountNumber.of("123456/0300");

            RegistrationService.RegisterNewMember command = new RegistrationService.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    null,
                    birthNumber,
                    bankAccountNumber
            );

            mockUserCreation(testSharedId);
            mockMemberCreation(testSharedId);

            // When
            Member result = service.registerMember(command);

            // Then
            assertThat(result.getId().toUserId().uuid()).isEqualTo(testSharedId.uuid());

            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getBirthNumber()).isNotNull();
            assertThat(savedMember.getBirthNumber().value()).isEqualTo("900101/1234");
            assertThat(savedMember.getBankAccountNumber()).isNotNull();
            assertThat(savedMember.getBankAccountNumber().value()).isEqualTo("123456/0300");
        }

        @Test
        @DisplayName("should register member with null birth number and bank account")
        void shouldRegisterMemberWithNullBirthNumberAndBankAccount() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2005, 6, 15);
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0500");
            Address address = Address.of("Hlavní 123", "Praha", "11000", "CZ");
            EmailAddress email = EmailAddress.of("jan.novak@example.com");
            PhoneNumber phone = PhoneNumber.of("+420777888999");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan", "Novák", dateOfBirth, "CZ", Gender.MALE
            );

            RegistrationService.RegisterNewMember command = new RegistrationService.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    null,
                    null,
                    null
            );

            mockUserCreation(testSharedId);
            mockMemberCreation(testSharedId);

            // When
            Member result = service.registerMember(command);

            // Then
            assertThat(result.getId().toUserId().uuid()).isEqualTo(testSharedId.uuid());

            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getBirthNumber()).isNull();
            assertThat(savedMember.getBankAccountNumber()).isNull();
        }
    }
}
