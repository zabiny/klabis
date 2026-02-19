package com.klabis.members.management;

import com.klabis.members.Gender;
import com.klabis.members.Member;
import com.klabis.members.MemberAssert;
import com.klabis.members.Members;
import com.klabis.members.persistence.MemberRepository;
import com.klabis.users.Authority;
import com.klabis.users.UserCreationParams;
import com.klabis.users.UserId;
import com.klabis.users.UserService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RegistrationService}.
 * <p>
 * Tests cover the member registration functionality including:
 * <ul>
 *   <li>Registration number generation</li>
 *   <li>User account creation with pending activation status</li>
 *   <li>Guardian information handling</li>
 *   <li>Sequential registration number generation</li>
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
    private Members membersMock;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationService service;

    private static final String CLUB_CODE = "ZBM";
    private static final String DEFAULT_HASHED_PASSWORD = "$2a$10$hashedPassword";

    @BeforeEach
    void setUp() {
        service = new RegistrationService(
                memberRepository,
                userService,
                passwordEncoder,
                membersMock,
                CLUB_CODE
        );

        // Setup default mock behavior that can be overridden in individual tests
        // Use a fixed shared ID for all tests by default
        UserId defaultSharedId = new UserId(UUID.fromString("12345678-1234-1234-1234-123456789012"));
        mockUserCreation(DEFAULT_HASHED_PASSWORD, defaultSharedId);
        mockMemberCreation(defaultSharedId);
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
    private void mockUserCreation(String hashedPassword, UserId sharedId) {
        // Mock both old method signature (3 params) and new one (1 UserCreationParams)
        when(userService.createUserPendingActivation(
                anyString(),
                anyString(),
                any(Set.class)
        )).thenReturn(sharedId);
        when(userService.createUserPendingActivation(any(UserCreationParams.class)))
                .thenReturn(sharedId);
        when(passwordEncoder.encode(anyString())).thenReturn(hashedPassword);
    }

    @Nested
    @DisplayName("registerMember() method")
    class RegisterMemberMethod {

        @Test
        @DisplayName("should generate registration number and create member")
        void shouldGenerateRegistrationNumberAndCreateMember() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2005, 6, 15);
            AddressRequest address = new AddressRequest(
                    "Hlavní 123",
                    "Praha",
                    "11000",
                    "CZ"
            );
            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE,
                    "jan.novak@example.com",
                    "+420777888999",
                    address,
                    null,
                    null,
                    null
            );

            when(membersMock.countByBirthYear(2005)).thenReturn(0);

            // When
            UUID memberId = service.registerMember(request);

            // Then - verify that an ID is returned
            assertThat(memberId).isNotNull();

            // Verify member repository interactions
            verify(membersMock).countByBirthYear(2005);
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            MemberAssert.assertThat(savedMember)
                    .hasFirstName("Jan")
                    .hasLastName("Novák")
                    .hasDateOfBirth(dateOfBirth)
                    .hasGender(Gender.MALE);
            assertThat(savedMember.getRegistrationNumber().getValue()).isEqualTo("ZBM0500");

            // Verify user creation via UserService with UserCreationParams
            ArgumentCaptor<UserCreationParams> paramsCaptor = ArgumentCaptor.forClass(UserCreationParams.class);
            verify(userService).createUserPendingActivation(paramsCaptor.capture());

            UserCreationParams params = paramsCaptor.getValue();
            assertThat(params.username()).isEqualTo("ZBM0500");
            assertThat(params.passwordHash()).isEqualTo(DEFAULT_HASHED_PASSWORD);
            assertThat(params.authorities()).isEqualTo(Set.of(Authority.MEMBERS_READ, Authority.EVENTS_READ));
            assertThat(params.getEmail()).contains("jan.novak@example.com");

            // CRITICAL: Verify Member ID equals the shared ID returned by UserService
            UserId defaultSharedId = new UserId(UUID.fromString("12345678-1234-1234-1234-123456789012"));
            assertThat(memberId).isEqualTo(defaultSharedId.uuid());
        }

        @Test
        @DisplayName("should create user account with pending activation status")
        void shouldCreateUserAccountWithPendingActivationStatus() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2005, 3, 20);
            AddressRequest address = new AddressRequest(
                    "Štúrova 45",
                    "Bratislava",
                    "81101",
                    "SK"
            );
            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Eva",
                    "Svobodová",
                    dateOfBirth,
                    "SK",
                    Gender.FEMALE,
                    "eva@example.com",
                    "+421777888999",
                    address,
                    null,
                    null,
                    null
            );

            when(membersMock.countByBirthYear(2005)).thenReturn(1);

            // Override default mock setup with specific values for this test
            UserId testSharedId = new UserId(UUID.fromString("87654321-4321-4321-4321-210987654321"));
            mockUserCreation("$2a$10$encodedPassword", testSharedId);
            mockMemberCreation(testSharedId);

            // When
            service.registerMember(request);

            // Then
            verify(passwordEncoder).encode(anyString());

            ArgumentCaptor<UserCreationParams> paramsCaptor = ArgumentCaptor.forClass(UserCreationParams.class);
            verify(userService).createUserPendingActivation(paramsCaptor.capture());

            UserCreationParams params = paramsCaptor.getValue();
            assertThat(params.username()).isEqualTo("ZBM0501");
            assertThat(params.passwordHash()).isEqualTo("$2a$10$encodedPassword");
            assertThat(params.authorities()).isEqualTo(Set.of(Authority.MEMBERS_READ, Authority.EVENTS_READ));
            assertThat(params.getEmail()).contains("eva@example.com");
        }

        @Test
        @DisplayName("should handle member with guardian information")
        void shouldHandleMemberWithGuardian() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2010, 1, 15);
            GuardianDTO guardianDTO = new GuardianDTO(
                    "Parent",
                    "Name",
                    "PARENT",
                    "parent@example.com",
                    "+420777111222"
            );

            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Child",
                    "Minor",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE,
                    "child@example.com",
                    "+420777333444",
                    MemberManagementDtosTestDataBuilder.addressRequestWithStreetAndCity("Dětská 1", "Brno"),
                    guardianDTO,
                    null,
                    null
            );

            when(membersMock.countByBirthYear(2010)).thenReturn(0);

            // Override default mock setup with specific values for this test
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            mockUserCreation("$2a$10$hash", testSharedId);
            mockMemberCreation(testSharedId);

            // When
            service.registerMember(request);

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
            AddressRequest address = new AddressRequest(
                    "Náměstí Svobody 1",
                    "Ostrava",
                    "70200",
                    "CZ"
            );
            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Adult",
                    "Member",
                    dateOfBirth,
                    "CZ",
                    Gender.FEMALE,
                    "adult@example.com",
                    "+420777555666",
                    address,
                    null,
                    null,
                    null
            );

            when(membersMock.countByBirthYear(1990)).thenReturn(5);

            // Override default mock setup with specific values for this test
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            mockUserCreation("$2a$10$hash", testSharedId);
            mockMemberCreation(testSharedId);

            // When
            service.registerMember(request);

            // Then
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getGuardian()).isNull();
        }

        @Test
        @DisplayName("should generate sequential registration numbers for same birth year")
        void shouldGenerateSequentialRegistrationNumbers() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2005, 7, 20);
            AddressRequest address = new AddressRequest(
                    "Testovací 5",
                    "Plzeň",
                    "30100",
                    "CZ"
            );
            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Test",
                    "Member",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE,
                    "test@example.com",
                    "+420777777777",
                    address,
                    null,
                    null,
                    null
            );

            // Simulate 2 existing members from 2005
            when(membersMock.countByBirthYear(2005)).thenReturn(2);

            // Override default mock setup with specific values for this test
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            mockUserCreation("$2a$10$hash", testSharedId);
            mockMemberCreation(testSharedId);

            // When
            service.registerMember(request);

            // Then
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            // Should be 3rd member (sequence 02) for birth year 2005
            assertThat(savedMember.getRegistrationNumber().getValue()).isEqualTo("ZBM0502");
        }

        @Test
        @DisplayName("should create user and member in same transaction")
        void shouldCreateUserAndMemberInSameTransaction() {
            // Given
            AddressRequest address = new AddressRequest(
                    "Transakční 10",
                    "Liberec",
                    "46001",
                    "CZ"
            );
            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Transaction",
                    "Test",
                    LocalDate.of(2000, 1, 1),
                    "CZ",
                    Gender.MALE,
                    "transaction@example.com",
                    "+420777000000",
                    address,
                    null,
                    null,
                    null
            );

            when(membersMock.countByBirthYear(2000)).thenReturn(0);

            // Override default mock setup with specific values for this test
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            mockUserCreation("$2a$10$hash", testSharedId);
            mockMemberCreation(testSharedId);

            // When
            service.registerMember(request);

            // Then - verify member repository and user service were called
            verify(memberRepository).save(any(Member.class));
            verify(userService).createUserPendingActivation(any(UserCreationParams.class));
        }

        @Test
        @DisplayName("should register member with birth number for Czech nationality")
        void shouldRegisterMemberWithBirthNumberForCzechNationality() {
            // Given
            LocalDate dateOfBirth = LocalDate.of(2005, 6, 15);
            AddressRequest address = new AddressRequest(
                    "Hlavní 123",
                    "Praha",
                    "11000",
                    "CZ"
            );
            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE,
                    "jan.novak@example.com",
                    "+420777888999",
                    address,
                    null,
                    "900101/1234",
                    null
            );

            when(membersMock.countByBirthYear(2005)).thenReturn(0);

            // Override default mock setup with specific values for this test
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            mockUserCreation("$2a$10$hash", testSharedId);
            mockMemberCreation(testSharedId);

            // When
            UUID memberId = service.registerMember(request);

            // Then
            assertThat(memberId).isEqualTo(testSharedId.uuid());

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
            AddressRequest address = new AddressRequest(
                    "Hlavní 123",
                    "Praha",
                    "11000",
                    "CZ"
            );
            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE,
                    "jan.novak@example.com",
                    "+420777888999",
                    address,
                    null,
                    null,
                    "CZ6508000000192000145399"
            );

            when(membersMock.countByBirthYear(2005)).thenReturn(0);

            // Override default mock setup with specific values for this test
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            mockUserCreation("$2a$10$hash", testSharedId);
            mockMemberCreation(testSharedId);

            // When
            UUID memberId = service.registerMember(request);

            // Then
            assertThat(memberId).isEqualTo(testSharedId.uuid());

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
            AddressRequest address = new AddressRequest(
                    "Hlavní 123",
                    "Praha",
                    "11000",
                    "CZ"
            );
            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE,
                    "jan.novak@example.com",
                    "+420777888999",
                    address,
                    null,
                    "900101/1234",
                    "123456/0300"
            );

            when(membersMock.countByBirthYear(2005)).thenReturn(0);

            // Override default mock setup with specific values for this test
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            mockUserCreation("$2a$10$hash", testSharedId);
            mockMemberCreation(testSharedId);

            // When
            UUID memberId = service.registerMember(request);

            // Then
            assertThat(memberId).isEqualTo(testSharedId.uuid());

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
            AddressRequest address = new AddressRequest(
                    "Hlavní 123",
                    "Praha",
                    "11000",
                    "CZ"
            );
            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Jan",
                    "Novák",
                    dateOfBirth,
                    "CZ",
                    Gender.MALE,
                    "jan.novak@example.com",
                    "+420777888999",
                    address,
                    null,
                    null,
                    null
            );

            when(membersMock.countByBirthYear(2005)).thenReturn(0);

            // Override default mock setup with specific values for this test
            UserId testSharedId = new UserId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
            mockUserCreation("$2a$10$hash", testSharedId);
            mockMemberCreation(testSharedId);

            // When
            UUID memberId = service.registerMember(request);

            // Then
            assertThat(memberId).isEqualTo(testSharedId.uuid());

            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());

            Member savedMember = memberCaptor.getValue();
            assertThat(savedMember.getBirthNumber()).isNull();
            assertThat(savedMember.getBankAccountNumber()).isNull();
        }
    }
}
