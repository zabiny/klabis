package com.klabis.members.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.common.users.UsernameAlreadyInUseException;
import com.klabis.common.users.application.PermissionService;
import com.klabis.members.MemberAssert;
import com.klabis.members.MemberId;
import com.klabis.members.domain.*;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardian.application.LegalGuardianPort.GuardianInput;
import com.klabis.members.legalguardian.application.LegalGuardianPort.NewLegalGuardian;
import com.klabis.members.legalguardian.domain.LegalGuardianEmailAlreadyInUseException;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationPort Unit Tests")
class RegistrationServiceTest {

    private static final UserId SHARED_ID = new UserId(UUID.fromString("12345678-1234-1234-1234-123456789012"));

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UserService userService;

    @Mock
    private LegalGuardianPort legalGuardianPort;

    @Mock
    private LegalGuardianGroupPort legalGuardianGroupPort;

    @Mock
    private MemberCompletenessPort memberCompletenessPort;

    @Mock
    private PermissionService permissionService;

    private RegistrationPort service;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(
                memberRepository,
                userService,
                new RegistrationNumberGenerator("ZBM", memberRepository),
                legalGuardianPort,
                legalGuardianGroupPort,
                memberCompletenessPort,
                permissionService
        );
    }

    private void stubUserCreation(UserId userId) {
        when(userService.createUser(anyString(), any(Set.class))).thenReturn(userId);
    }

    private void stubMemberSave() {
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("registerMember() method")
    class RegisterMemberMethod {

        @Test
        @DisplayName("should create member and user with generated registration number and all optional fields")
        void shouldCreateMemberWithGeneratedRegistrationNumber() {
            LocalDate dateOfBirth = LocalDate.of(2005, 6, 15);
            when(memberRepository.countByBirthYear(2005)).thenReturn(7);
            stubUserCreation(SHARED_ID);
            stubMemberSave();

            RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(
                    PersonalInformation.of("Jan", "Novák", dateOfBirth, Nationality.of("CZ"), Gender.MALE),
                    Address.of("Hlavní 123", "Praha", "11000", Country.of("CZ")),
                    EmailAddress.of("jan.novak@example.com"),
                    PhoneNumber.of("+420777888999"),
                    BirthNumber.of("050615/1234"),
                    BankAccountNumber.of("CZ6508000000192000145399"),
                    null
            );

            Member result = service.registerMember(command);

            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());
            Member savedMember = memberCaptor.getValue();
            MemberAssert.assertThat(savedMember)
                    .hasFirstName("Jan")
                    .hasLastName("Novák")
                    .hasDateOfBirth(dateOfBirth)
                    .hasGender(Gender.MALE);
            assertThat(savedMember.getRegistrationNumber().getValue()).isEqualTo("ZBM0507");
            assertThat(savedMember.getBirthNumber().value()).isEqualTo("050615/1234");
            assertThat(savedMember.getBankAccountNumber().value()).isEqualTo("CZ6508000000192000145399");

            verify(userService).createUser("ZBM0507", Set.of(Authority.MEMBERS_READ, Authority.EVENTS_READ));
            assertThat(result.getId().toUserId()).isEqualTo(SHARED_ID);
        }

        @Test
        @DisplayName("should register non-CZ member without birth number and bank account")
        void shouldRegisterNonCzechMemberWithoutOptionalFields() {
            stubUserCreation(SHARED_ID);
            stubMemberSave();

            RegistrationPort.RegisterNewMember command = new RegistrationPort.RegisterNewMember(
                    PersonalInformation.of("Eva", "Svobodová", LocalDate.of(2005, 3, 20), Nationality.of("SK"), Gender.FEMALE),
                    Address.of("Štúrova 45", "Bratislava", "81101", Country.of("SK")),
                    EmailAddress.of("eva@example.com"),
                    PhoneNumber.of("+421777888999"),
                    null,
                    null,
                    null
            );

            Member result = service.registerMember(command);

            assertThat(result.getId().toUserId()).isEqualTo(SHARED_ID);
            assertThat(result.getBirthNumber()).isNull();
            assertThat(result.getBankAccountNumber()).isNull();
            verify(userService).createUser(eq("ZBM0500"), any(Set.class));
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
                    PersonalInformation.of("Child", "Minor", LocalDate.now().minusYears(10), Nationality.of("CZ"), Gender.MALE),
                    Address.of("Dětská 1", "Brno", "60200", Country.of("CZ")),
                    email,
                    phone,
                    BirthNumber.of("150410/1234"),
                    null,
                    null,
                    guardians,
                    null);
        }

        @Test
        @DisplayName("should register a minor without own contacts when a guardian covers them")
        void shouldRegisterMinorWithoutOwnContactsWhenGuardianCoversThem() {
            List<GuardianInput> guardians = List.of(GuardianInput.existing(GUARDIAN_ID));
            when(legalGuardianPort.resolveGuardians(guardians)).thenReturn(Set.of(GUARDIAN_ID));
            when(memberCompletenessPort.contactsOfChosenGuardians(Set.of(GUARDIAN_ID)))
                    .thenReturn(new GuardianContacts(true, true, true));

            stubUserCreation(MINOR_ID);
            stubMemberSave();

            Member result = service.registerMember(minorCommand(null, null, guardians));

            assertThat(result.getEmail()).isNull();
            assertThat(result.isDataIncomplete()).isFalse();
            verify(userService).createUser(result.getRegistrationNumber().getValue(),
                    Set.of(Authority.MEMBERS_READ, Authority.EVENTS_READ));
        }

        @Test
        @DisplayName("should set guardians of the minor after the member is saved")
        void shouldSetGuardiansAfterMemberIsSaved() {
            List<GuardianInput> guardians = List.of(GuardianInput.existing(GUARDIAN_ID));
            when(legalGuardianPort.resolveGuardians(guardians)).thenReturn(Set.of(GUARDIAN_ID));
            when(memberCompletenessPort.contactsOfChosenGuardians(Set.of(GUARDIAN_ID)))
                    .thenReturn(new GuardianContacts(true, true, true));
            stubUserCreation(MINOR_ID);
            stubMemberSave();

            service.registerMember(minorCommand(null, null, guardians));

            var order = inOrder(memberRepository, legalGuardianGroupPort);
            order.verify(memberRepository).save(any(Member.class));
            order.verify(legalGuardianGroupPort).setGuardiansOf(MemberId.fromUserId(MINOR_ID), Set.of(GUARDIAN_ID));
        }

        @Test
        @DisplayName("should reject a minor without any guardian and save no member")
        void shouldRejectMinorWithoutGuardianAndSaveNoMember() {
            stubUserCreation(MINOR_ID);

            assertThatThrownBy(() -> service.registerMember(
                    minorCommand(EmailAddress.of("child@example.com"), PhoneNumber.of("+420777333444"), List.of())))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Guardian is required");

            verify(memberRepository, never()).save(any(Member.class));
            verify(legalGuardianGroupPort, never()).setGuardiansOf(any(MemberId.class), any(Set.class));
        }

        @Test
        @DisplayName("should reject a minor whose contacts nobody provides")
        void shouldRejectMinorWithoutContacts() {
            List<GuardianInput> guardians = List.of(GuardianInput.existing(GUARDIAN_ID));
            when(legalGuardianPort.resolveGuardians(guardians)).thenReturn(Set.of(GUARDIAN_ID));
            when(memberCompletenessPort.contactsOfChosenGuardians(Set.of(GUARDIAN_ID)))
                    .thenReturn(new GuardianContacts(true, false, false));
            stubUserCreation(MINOR_ID);

            assertThatThrownBy(() -> service.registerMember(minorCommand(null, null, guardians)))
                    .isInstanceOf(MemberDataRequiredException.class);

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
                    PersonalInformation.of("Child", "Minor", LocalDate.now().minusYears(10), Nationality.of("CZ"), Gender.MALE),
                    Address.of("Dětská 1", "Brno", "60200", Country.of("CZ")), null, null, null, null, null,
                    List.of(GuardianInput.existing(GUARDIAN_ID)), GUARDIAN_ID);

            assertThatThrownBy(() -> service.registerMember(command))
                    .isInstanceOf(GuardiansNotAllowedException.class)
                    .satisfies(e -> {
                        var ex = (GuardiansNotAllowedException) e;
                        assertThat(ex.errorType()).isEqualTo("minor-cannot-take-over-guardian");
                        assertThat(ex.field()).isEqualTo("legalGuardianUserId");
                    });

            verify(memberRepository, never()).save(any(Member.class));
            verify(legalGuardianPort, never()).releaseForMembership(any(UserId.class));
            verify(permissionService, never()).updateUserPermissions(any(UserId.class), any());
            verify(userService, never()).createUser(anyString(), any(Set.class));
            verify(userService, never()).replaceUsername(any(UserId.class), anyString());
        }
    }

    @Nested
    @DisplayName("registerMember() of an adult")
    class RegisterAdult {

        private RegistrationPort.RegisterNewMember adultCommand(EmailAddress email, PhoneNumber phone,
                                                               List<GuardianInput> guardians, UserId takeOver) {
            return new RegistrationPort.RegisterNewMember(
                    PersonalInformation.of("Eva", "Svobodová", LocalDate.of(1985, 4, 10), Nationality.of("CZ"), Gender.FEMALE),
                    Address.of("Hlavní 1", "Brno", "60200", Country.of("CZ")),
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
        @DisplayName("should not touch legal guardian groups when registering an adult")
        void shouldNotTouchGroupsForAdult() {
            stubUserCreation(SHARED_ID);
            stubMemberSave();
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
                    PersonalInformation.of("Eva", "Svobodová", LocalDate.of(1985, 4, 10), Nationality.of("CZ"), Gender.FEMALE),
                    Address.of("Hlavní 1", "Brno", "60200", Country.of("CZ")),
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
            stubMemberSave();
            Member result = service.registerMember(takeOverCommand());

            assertThat(result.getId()).isEqualTo(MemberId.fromUserId(GUARDIAN_ID));
            assertThat(result.getRegistrationNumber().getValue()).isEqualTo("ZBM8500");
            verify(userService, never()).createUser(anyString(), any(Set.class));
        }

        @Test
        @DisplayName("should make the generated registration number the username of the guardian's user")
        void shouldReplaceUsernameWithRegistrationNumber() {
            stubMemberSave();
            service.registerMember(takeOverCommand());

            verify(userService).replaceUsername(GUARDIAN_ID, "ZBM8500");
        }

        @Test
        @DisplayName("should report a registration number already used as someone's login as registration number in use")
        void shouldMapLoginCollisionToRegistrationNumberInUse() {
            doThrow(new UsernameAlreadyInUseException("ZBM8500"))
                    .when(userService).replaceUsername(GUARDIAN_ID, "ZBM8500");

            assertThatThrownBy(() -> service.registerMember(takeOverCommand()))
                    .isInstanceOf(RegistrationNumberAlreadyInUseException.class);
            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("should remove the guardian profile before the member is saved")
        void shouldReleaseGuardianProfileBeforeSavingMember() {
            stubMemberSave();
            service.registerMember(takeOverCommand());

            var order = inOrder(legalGuardianPort, memberRepository);
            order.verify(legalGuardianPort).releaseForMembership(GUARDIAN_ID);
            order.verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("should grant the standard member authorities to the guardian's user")
        void shouldGrantMemberAuthorities() {
            stubMemberSave();
            service.registerMember(takeOverCommand());

            verify(permissionService).updateUserPermissions(GUARDIAN_ID, Authority.getStandardUserAuthorities());
        }

        @Test
        @DisplayName("should leave the legal guardian groups untouched")
        void shouldLeaveGroupsUntouched() {
            stubMemberSave();
            service.registerMember(takeOverCommand());

            verify(legalGuardianGroupPort, never()).setGuardiansOf(any(MemberId.class), any(Set.class));
            verify(legalGuardianGroupPort, never()).changeGroupGuardians(any(), any(Set.class));
        }
    }

    @Nested
    @DisplayName("importMember() method")
    class ImportMemberMethod {

        private RegistrationPort.ImportMember importCommand() {
            RegistrationPort.RegisterNewMember details = new RegistrationPort.RegisterNewMember(
                    PersonalInformation.of("Jan", "Novák", LocalDate.of(2005, 6, 15), Nationality.of("CZ"), Gender.MALE),
                    Address.of("Hlavní 123", "Praha", "11000", Country.of("CZ")),
                    EmailAddress.of("jan.novak@example.com"),
                    PhoneNumber.of("+420777888999"),
                    BirthNumber.of("050615/1234"),
                    null,
                    null
            );
            return new RegistrationPort.ImportMember(details, RegistrationNumber.of("ZBM0099"));
        }

        @Test
        @DisplayName("should create member and user with the given registration number instead of a generated one")
        void shouldCreateMemberWithGivenRegistrationNumber() {
            stubUserCreation(SHARED_ID);
            stubMemberSave();

            Member result = service.importMember(importCommand());

            assertThat(result.getRegistrationNumber()).isEqualTo(RegistrationNumber.of("ZBM0099"));
            verify(userService).createUser(eq("ZBM0099"), any(Set.class));
            verify(memberRepository, never()).countByBirthYear(anyInt());
        }

        @Test
        @DisplayName("should report a database uniqueness violation as registration number in use")
        void shouldMapUniqueConstraintViolationToRegistrationNumberInUse() {
            stubUserCreation(SHARED_ID);
            when(memberRepository.save(any(Member.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

            assertThatThrownBy(() -> service.importMember(importCommand()))
                    .isInstanceOf(RegistrationNumberAlreadyInUseException.class)
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
