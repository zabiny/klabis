package com.klabis.members.application;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.application.PermissionService;
import com.klabis.members.MemberAssert;
import com.klabis.members.MemberId;
import com.klabis.members.domain.*;
import com.klabis.members.legalguardian.application.GuardianContact;
import com.klabis.members.legalguardian.application.GuardianContactResolver;
import com.klabis.members.legalguardian.application.GuardianKind;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardian.application.LegalGuardianPort.GuardianInput;
import com.klabis.members.legalguardian.application.LegalGuardianPort.NewLegalGuardian;
import com.klabis.members.legalguardian.domain.LegalGuardianEmailAlreadyInUseException;
import com.klabis.members.legalguardiangroup.application.GuardianNotFoundException;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RegistrationService}.
 * <p>
 * Tests cover the member registration functionality including:
 * <ul>
 *   <li>User account creation with pending activation status</li>
 *   <li>Transactional integrity of member and user creation</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RegistrationPort Unit Tests")
class RegistrationServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UserService userService;

    @Mock
    private RegistrationNumberGenerator registrationNumberGenerator;

    @Mock
    private LegalGuardianPort legalGuardianPort;

    @Mock
    private LegalGuardianGroupPort legalGuardianGroupPort;

    @Mock
    private MemberCompletenessPort memberCompletenessPort;

    @Mock
    private PermissionService permissionService;

    @Mock
    private GuardianContactResolver guardianContactResolver;

    private RegistrationPort service;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(
                memberRepository,
                userService,
                registrationNumberGenerator,
                legalGuardianPort,
                legalGuardianGroupPort,
                memberCompletenessPort,
                permissionService,
                guardianContactResolver
        );

        // Setup default mock behavior that can be overridden in individual tests
        // Use a fixed shared ID for all tests by default
        UserId defaultSharedId = new UserId(UUID.fromString("12345678-1234-1234-1234-123456789012"));
        mockUserCreation(defaultSharedId);
        mockMemberCreation(defaultSharedId);

        when(guardianContactResolver.resolve(any())).thenAnswer(invocation -> {
            java.util.Collection<UserId> ids = invocation.getArgument(0);
            return ids.stream().map(id -> new GuardianContact(id, "G", "Guardian", "g@example.com",
                    "+420777000000", GuardianKind.MEMBER)).toList();
        });

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
        when(userService.createUser(anyString(), any(Set.class))).thenReturn(sharedId);
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

            RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    BirthNumber.of("050615/1234"),
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
            ArgumentCaptor<Set> authoritiesCaptor = ArgumentCaptor.forClass(Set.class);
            verify(userService).createUser(usernameCaptor.capture(), authoritiesCaptor.capture());

            assertThat(usernameCaptor.getValue()).isEqualTo("ZBM0500");
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

            RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(personalInformation,
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
            ArgumentCaptor<Set> authoritiesCaptor = ArgumentCaptor.forClass(Set.class);
            verify(userService).createUser(usernameCaptor.capture(), authoritiesCaptor.capture());

            assertThat(usernameCaptor.getValue()).isEqualTo("ZBM0501");
            assertThat(authoritiesCaptor.getValue()).isEqualTo(Set.of(Authority.MEMBERS_READ, Authority.EVENTS_READ));
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

            RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    BirthNumber.of("050720/1234"),
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

            RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    BirthNumber.of("000101/1234"),
                    null,
                    null
            );

            mockUserCreation(testSharedId);
            mockMemberCreation(testSharedId);

            // When
            service.registerMember(command);

            // Then - verify member repository and user service were called
            verify(memberRepository).save(any(Member.class));
            verify(userService).createUser(anyString(), any(Set.class));
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

            RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    birthNumber,
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
                    "Jan", "Novák", dateOfBirth, "SK", Gender.MALE
            );
            BankAccountNumber bankAccountNumber = BankAccountNumber.of("CZ6508000000192000145399");

            RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    null,
                    bankAccountNumber,
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

            RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    birthNumber,
                    bankAccountNumber,
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
            assertThat(savedMember.getBankAccountNumber()).isNotNull();
            assertThat(savedMember.getBankAccountNumber().value()).isEqualTo("123456/0300");
        }

        @Test
        @DisplayName("should register non-CZ member with null birth number and bank account")
        void shouldRegisterMemberWithNullBirthNumberAndBankAccount() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2005, 6, 15);
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            RegistrationNumber registrationNumber = new RegistrationNumber("ZBM0500");
            Address address = Address.of("Hlavní 123", "Praha", "11000", "CZ");
            EmailAddress email = EmailAddress.of("jan.novak@example.com");
            PhoneNumber phone = PhoneNumber.of("+420777888999");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan", "Novák", dateOfBirth, "SK", Gender.MALE
            );

            RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(personalInformation,
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

        @Test
        @DisplayName("should generate a registration number as before (regression guard)")
        void shouldStillGenerateRegistrationNumber() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2005, 6, 15);
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            Address address = Address.of("Hlavní 123", "Praha", "11000", "CZ");
            EmailAddress email = EmailAddress.of("jan.novak@example.com");
            PhoneNumber phone = PhoneNumber.of("+420777888999");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan", "Novák", dateOfBirth, "CZ", Gender.MALE
            );

            // RegisterNewMember carries no registration number field - it must stay this shape
            RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    BirthNumber.of("050615/1234"),
                    null,
                    null
            );

            mockUserCreation(testSharedId);
            mockMemberCreation(testSharedId);

            // When
            Member result = service.registerMember(command);

            // Then
            verify(registrationNumberGenerator).generate(dateOfBirth);
            assertThat(result.getRegistrationNumber().getValue()).isEqualTo("ZBM0500");
        }
    }

    @Nested
    @DisplayName("registerMember() of a minor")
    class RegisterMinor {

        private static final UserId GUARDIAN_ID = new UserId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        private static final UserId MINOR_ID = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));

        private RegistrationPort.RegisterNewMember minorCommand(EmailAddress email, PhoneNumber phone,
                                                               List<GuardianInput> guardians) {
            return new RegistrationPort.RegisterNewMember(
                    PersonalInformation.of("Child", "Minor", LocalDate.of(2015, 4, 10), "CZ", Gender.MALE),
                    Address.of("Dětská 1", "Brno", "60200", "CZ"),
                    email,
                    phone,
                    BirthNumber.of("150410/1234"),
                    null,
                    null,
                    guardians,
                    null);
        }

        @BeforeEach
        void setUpMinor() {
            mockUserCreation(MINOR_ID);
            mockMemberCreation(MINOR_ID);
        }

        @Test
        @DisplayName("should register a minor without own contacts when a guardian covers them")
        void shouldRegisterMinorWithoutOwnContactsWhenGuardianCoversThem() {
            List<GuardianInput> guardians = List.of(GuardianInput.existing(GUARDIAN_ID));
            when(legalGuardianPort.resolveGuardians(guardians)).thenReturn(Set.of(GUARDIAN_ID));
            when(memberCompletenessPort.contactsOfGuardians(Set.of(GUARDIAN_ID)))
                    .thenReturn(new GuardianContacts(true, true, true));

            Member result = service.registerMember(minorCommand(null, null, guardians));

            assertThat(result.getEmail()).isNull();
            assertThat(result.isDataIncomplete()).isFalse();
            verify(userService).createUser(eq("ZBM0500"), eq(Set.of(Authority.MEMBERS_READ, Authority.EVENTS_READ)));
        }

        @Test
        @DisplayName("should set guardians of the minor after the member is saved")
        void shouldSetGuardiansAfterMemberIsSaved() {
            List<GuardianInput> guardians = List.of(GuardianInput.existing(GUARDIAN_ID));
            when(legalGuardianPort.resolveGuardians(guardians)).thenReturn(Set.of(GUARDIAN_ID));
            when(memberCompletenessPort.contactsOfGuardians(Set.of(GUARDIAN_ID)))
                    .thenReturn(new GuardianContacts(true, true, true));

            service.registerMember(minorCommand(null, null, guardians));

            var order = inOrder(memberRepository, legalGuardianGroupPort);
            order.verify(memberRepository).save(any(Member.class));
            order.verify(legalGuardianGroupPort).setGuardiansOf(MemberId.fromUserId(MINOR_ID), Set.of(GUARDIAN_ID));
        }

        @Test
        @DisplayName("should register a minor with a new guardian resolved together with the registration")
        void shouldRegisterMinorWithNewGuardian() {
            List<GuardianInput> guardians = List.of(GuardianInput.created(
                    new NewLegalGuardian("Eva", "Svobodová", "eva@example.com", "+420777111222")));
            when(legalGuardianPort.resolveGuardians(guardians)).thenReturn(Set.of(GUARDIAN_ID));
            when(memberCompletenessPort.contactsOfGuardians(Set.of(GUARDIAN_ID)))
                    .thenReturn(new GuardianContacts(true, true, true));

            service.registerMember(minorCommand(null, null, guardians));

            verify(legalGuardianGroupPort).setGuardiansOf(MemberId.fromUserId(MINOR_ID), Set.of(GUARDIAN_ID));
        }

        @Test
        @DisplayName("should reject a minor without any guardian and create nothing")
        void shouldRejectMinorWithoutGuardian() {
            assertThatThrownBy(() -> service.registerMember(
                    minorCommand(EmailAddress.of("child@example.com"), PhoneNumber.of("+420777333444"), List.of())))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Guardian is required");

            verify(memberRepository, never()).save(any(Member.class));
            verify(legalGuardianGroupPort, never()).setGuardiansOf(any(MemberId.class), any(Set.class));
        }

        @Test
        @DisplayName("should reject a chosen guardian who is unknown or a minor with a specific error")
        void shouldRejectUnusableGuardian() {
            List<GuardianInput> guardians = List.of(GuardianInput.existing(GUARDIAN_ID));
            when(legalGuardianPort.resolveGuardians(guardians)).thenReturn(Set.of(GUARDIAN_ID));
            when(guardianContactResolver.resolve(Set.of(GUARDIAN_ID))).thenReturn(List.of());

            assertThatThrownBy(() -> service.registerMember(minorCommand(null, null, guardians)))
                    .isInstanceOf(GuardianNotFoundException.class);

            verify(userService, never()).createUser(anyString(), any(Set.class));
            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("should reject a minor whose contacts nobody provides")
        void shouldRejectMinorWithoutContacts() {
            List<GuardianInput> guardians = List.of(GuardianInput.existing(GUARDIAN_ID));
            when(legalGuardianPort.resolveGuardians(guardians)).thenReturn(Set.of(GUARDIAN_ID));
            when(memberCompletenessPort.contactsOfGuardians(Set.of(GUARDIAN_ID)))
                    .thenReturn(new GuardianContacts(true, false, false));

            assertThatThrownBy(() -> service.registerMember(minorCommand(null, null, guardians)))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("should not create the member when creating a guardian fails")
        void shouldCreateNothingWhenGuardianCreationFails() {
            List<GuardianInput> guardians = List.of(GuardianInput.created(
                    new NewLegalGuardian("Eva", "Svobodová", "eva@example.com", "+420777111222")));
            when(legalGuardianPort.resolveGuardians(guardians))
                    .thenThrow(new LegalGuardianEmailAlreadyInUseException("eva@example.com"));

            assertThatThrownBy(() -> service.registerMember(minorCommand(null, null, guardians)))
                    .isInstanceOf(LegalGuardianEmailAlreadyInUseException.class);

            verify(userService, never()).createUser(anyString(), any(Set.class));
            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("should reject taking over a legal guardian for a minor")
        void shouldRejectTakeOverForMinor() {
            var command = new RegistrationPort.RegisterNewMember(
                    PersonalInformation.of("Child", "Minor", LocalDate.of(2015, 4, 10), "CZ", Gender.MALE),
                    Address.of("Dětská 1", "Brno", "60200", "CZ"), null, null, null, null, null,
                    List.of(GuardianInput.existing(GUARDIAN_ID)), GUARDIAN_ID);

            assertThatThrownBy(() -> service.registerMember(command))
                    .isInstanceOf(BusinessRuleViolationException.class);

            verify(memberRepository, never()).save(any(Member.class));
            verify(legalGuardianPort, never()).releaseForMembership(any(UserId.class));
        }
    }

    @Nested
    @DisplayName("registerMember() of an adult")
    class RegisterAdult {

        private RegistrationPort.RegisterNewMember adultCommand(EmailAddress email, PhoneNumber phone,
                                                               List<GuardianInput> guardians, UserId takeOver) {
            return new RegistrationPort.RegisterNewMember(
                    PersonalInformation.of("Eva", "Svobodová", LocalDate.of(1985, 4, 10), "CZ", Gender.FEMALE),
                    Address.of("Hlavní 1", "Brno", "60200", "CZ"),
                    email,
                    phone,
                    BirthNumber.of("855410/1234"),
                    null,
                    null,
                    guardians,
                    takeOver);
        }

        @Test
        @DisplayName("should reject an adult with legal guardians and create nothing")
        void shouldRejectAdultWithGuardians() {
            var command = adultCommand(EmailAddress.of("eva@example.com"), PhoneNumber.of("+420777111222"),
                    List.of(GuardianInput.existing(new UserId(UUID.randomUUID()))), null);

            assertThatThrownBy(() -> service.registerMember(command))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Adult");

            verify(legalGuardianPort, never()).resolveGuardians(any());
            verify(userService, never()).createUser(anyString(), any(Set.class));
            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("should require own e-mail of an adult")
        void shouldRequireOwnEmail() {
            assertThatThrownBy(() -> service.registerMember(
                    adultCommand(null, PhoneNumber.of("+420777111222"), List.of(), null)))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("should require own phone of an adult")
        void shouldRequireOwnPhone() {
            assertThatThrownBy(() -> service.registerMember(
                    adultCommand(EmailAddress.of("eva@example.com"), null, List.of(), null)))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("should not touch legal guardian groups when registering an adult")
        void shouldNotTouchGroupsForAdult() {
            service.registerMember(adultCommand(EmailAddress.of("eva@example.com"),
                    PhoneNumber.of("+420777111222"), List.of(), null));

            verify(legalGuardianGroupPort, never()).setGuardiansOf(any(MemberId.class), any(Set.class));
        }
    }

    @Nested
    @DisplayName("registerMember() taking over a non-member legal guardian")
    class TakeOverGuardian {

        private static final UserId GUARDIAN_ID = new UserId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

        private RegistrationPort.RegisterNewMember takeOverCommand() {
            return new RegistrationPort.RegisterNewMember(
                    PersonalInformation.of("Eva", "Svobodová", LocalDate.of(1985, 4, 10), "CZ", Gender.FEMALE),
                    Address.of("Hlavní 1", "Brno", "60200", "CZ"),
                    EmailAddress.of("eva@example.com"),
                    PhoneNumber.of("+420777111222"),
                    BirthNumber.of("855410/1234"),
                    null,
                    null,
                    List.of(),
                    GUARDIAN_ID);
        }

        @Test
        @DisplayName("should register the member under the user id of the guardian without creating a new user")
        void shouldReuseUserOfGuardian() {
            Member result = service.registerMember(takeOverCommand());

            assertThat(result.getId()).isEqualTo(MemberId.fromUserId(GUARDIAN_ID));
            assertThat(result.getRegistrationNumber().getValue()).isEqualTo("ZBM0500");
            verify(userService, never()).createUser(anyString(), any(Set.class));
        }

        @Test
        @DisplayName("should remove the guardian profile before the member is saved")
        void shouldReleaseGuardianProfileBeforeSavingMember() {
            service.registerMember(takeOverCommand());

            var order = inOrder(legalGuardianPort, memberRepository);
            order.verify(legalGuardianPort).releaseForMembership(GUARDIAN_ID);
            order.verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("should grant the standard member authorities to the guardian's user")
        void shouldGrantMemberAuthorities() {
            service.registerMember(takeOverCommand());

            verify(permissionService).updateUserPermissions(GUARDIAN_ID, Authority.getStandardUserAuthorities());
        }

        @Test
        @DisplayName("should leave the legal guardian groups untouched")
        void shouldLeaveGroupsUntouched() {
            service.registerMember(takeOverCommand());

            verify(legalGuardianGroupPort, never()).setGuardiansOf(any(MemberId.class), any(Set.class));
            verify(legalGuardianGroupPort, never()).changeGroupGuardians(any(), any(Set.class));
        }
    }

    @Nested
    @DisplayName("importMember() method")
    class ImportMemberMethod {

        @Test
        @DisplayName("should create member with the given registration number and never call the generator")
        void shouldCreateMemberWithGivenRegistrationNumberAndNeverGenerate() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2005, 6, 15);
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            Address address = Address.of("Hlavní 123", "Praha", "11000", "CZ");
            EmailAddress email = EmailAddress.of("jan.novak@example.com");
            PhoneNumber phone = PhoneNumber.of("+420777888999");
            PersonalInformation personalInformation = PersonalInformation.of(
                    "Jan", "Novák", dateOfBirth, "CZ", Gender.MALE
            );
            RegistrationNumber importedRegistrationNumber = RegistrationNumber.of("ZBM0099");

            RegistrationPort.RegisterNewMember details = new RegistrationPort.RegisterNewMember(personalInformation,
                    address,
                    email,
                    phone,
                    BirthNumber.of("050615/1234"),
                    null,
                    null
            );
            RegistrationPort.ImportMember command = new RegistrationPort.ImportMember(details, importedRegistrationNumber);

            mockUserCreation(testSharedId);
            mockMemberCreation(testSharedId);

            // When
            Member result = service.importMember(command);

            // Then
            assertThat(result.getRegistrationNumber()).isEqualTo(importedRegistrationNumber);
            verify(registrationNumberGenerator, never()).generate(any(LocalDate.class));

            ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
            verify(userService).createUser(usernameCaptor.capture(), any(Set.class));
            assertThat(usernameCaptor.getValue()).isEqualTo("ZBM0099");
        }
    }
}
