package com.klabis.members.management;

import com.klabis.members.MemberTerminatedEvent;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.*;
import com.klabis.members.infrastructure.restapi.TerminateMembershipRequest;
import com.klabis.users.UserId;
import com.klabis.users.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ManagementService Unit Tests")
class ManagementServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UserService userService;

    private ManagementService testedSubject;
    private UUID testMemberId;
    private Member testMember;

    @BeforeEach
    void setUp() {
        testedSubject = new ManagementServiceImpl(memberRepository, userService);

        testMemberId = UUID.randomUUID();
        testMember = MemberTestDataBuilder.aMember()
                .withId(testMemberId)
                .withFirstName("John")
                .withLastName("Doe")
                .withRegistrationNumber("ZBM1234")
                .withEmail("john.doe@example.com")
                .withPhone("+420123456789")
                .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                .withNoGuardian()
                .build();
    }

    @Nested
    @DisplayName("Member Update — Admin")
    class AdminMemberUpdateTests {

        @Test
        @DisplayName("should update member birth number via admin command")
        void shouldUpdateMemberWithBirthNumberViaAdminCommand() {
            var command = new Member.UpdateMemberByAdmin(
                    null, null, null, null, null,
                    BankAccountNumber.of("12345/5678"),
                    null, null, null, null, null, null,
                    null, null, null, null,
                    BirthNumber.of("900101/1234")
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(Optional.of(testMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            Member result = testedSubject.updateMember(testMemberId, command);

            assertThat(result.getId().uuid()).isEqualTo(testMemberId);
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("should update member bank account number via admin command")
        void shouldUpdateMemberWithBankAccountNumberViaAdminCommand() {
            var command = new Member.UpdateMemberByAdmin(
                    null, null, null, null, null,
                    BankAccountNumber.of("12345/5678"),
                    null, null, null, null, null, null,
                    null, null, null, null, null
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(Optional.of(testMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            Member result = testedSubject.updateMember(testMemberId, command);

            assertThat(result.getId().uuid()).isEqualTo(testMemberId);
            verify(memberRepository).save(any(Member.class));
        }
    }

    @Nested
    @DisplayName("Member Update — Self")
    class SelfMemberUpdateTests {

        @Test
        @DisplayName("should update member contact info via self-update command")
        void shouldUpdateMemberContactInfoViaSelfUpdateCommand() {
            var command = new Member.SelfUpdate(
                    EmailAddress.of("new@example.com"),
                    null, null, null, null, null, null, null, null, null, null, null
            );

            when(memberRepository.findById(new UserId(testMemberId))).thenReturn(Optional.of(testMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            Member result = testedSubject.updateMember(testMemberId, command);

            assertThat(result.getId().uuid()).isEqualTo(testMemberId);
            verify(memberRepository).save(any(Member.class));
        }
    }

    @Nested
    @DisplayName("Member Termination Tests")
    class MemberTerminationTests {

        private Member testActiveMember;
        private UUID adminUserId;

        @BeforeEach
        void setUpNested() {
            adminUserId = UUID.randomUUID();
            testActiveMember = MemberTestDataBuilder.aMember()
                    .withId(testMemberId)
                    .withFirstName("Jane")
                    .withLastName("Smith")
                    .withRegistrationNumber("ZBM5678")
                    .withEmail("jane.smith@example.com")
                    .withPhone("+420987654321")
                    .withAddress(Address.of("Vinohradská 456", "Praha", "12000", "CZ"))
                    .withNoGuardian()
                    .build();

            UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                    "admin", "password",
                    List.of(new SimpleGrantedAuthority("MEMBERS:UPDATE"))
            );
            SecurityContextHolder.getContext().setAuthentication(adminAuth);
        }

        @AfterEach
        void tearDownNested() {
            SecurityContextHolder.clearContext();
        }

        @Nested
        @DisplayName("Successful Termination Tests")
        class SuccessfulTerminationTests {

            @Test
            @DisplayName("should terminate active member with ODHLASKA reason")
            void shouldTerminateActiveMemberWithOdhlaskaReason() {
                var request = new TerminateMembershipRequest(
                        DeactivationReason.ODHLASKA,
                        Optional.of("Member requested resignation")
                );

                when(memberRepository.findById(new UserId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

                var command = new Member.TerminateMembership(
                        new UserId(adminUserId), request.reason(), request.note().orElse(null)
                );
                Member result = testedSubject.terminateMember(testMemberId, command);

                assertThat(result.getId().uuid()).isEqualTo(testMemberId);

                ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
                verify(memberRepository).save(captor.capture());

                Member saved = captor.getValue();
                assertThat(saved.isActive()).isFalse();
                assertThat(saved.getDeactivationReason()).isEqualTo(DeactivationReason.ODHLASKA);
                assertThat(saved.getDeactivatedAt()).isNotNull();
                assertThat(saved.getDeactivationNote()).isEqualTo("Member requested resignation");
                assertThat(saved.getDeactivatedBy().uuid()).isEqualTo(adminUserId);
            }

            @Test
            @DisplayName("should terminate active member without note")
            void shouldTerminateActiveMemberWithoutNote() {
                var request = new TerminateMembershipRequest(
                        DeactivationReason.PRESTUP, Optional.empty()
                );

                when(memberRepository.findById(new UserId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

                var command = new Member.TerminateMembership(
                        new UserId(adminUserId), request.reason(), request.note().orElse(null)
                );
                Member result = testedSubject.terminateMember(testMemberId, command);

                assertThat(result.getId().uuid()).isEqualTo(testMemberId);

                ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
                verify(memberRepository).save(captor.capture());

                assertThat(captor.getValue().isActive()).isFalse();
                assertThat(captor.getValue().getDeactivationReason()).isEqualTo(DeactivationReason.PRESTUP);
                assertThat(captor.getValue().getDeactivationNote()).isNull();
            }

            @Test
            @DisplayName("should publish MemberTerminatedEvent on successful termination")
            void shouldPublishMemberTerminatedEventOnSuccessfulTermination() {
                var request = new TerminateMembershipRequest(
                        DeactivationReason.OTHER, Optional.of("Administrative decision")
                );

                when(memberRepository.findById(new UserId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

                var command = new Member.TerminateMembership(
                        new UserId(adminUserId), request.reason(), request.note().orElse(null)
                );
                testedSubject.terminateMember(testMemberId, command);

                ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
                verify(memberRepository).save(captor.capture());

                Member saved = captor.getValue();
                assertThat(saved.getDomainEvents()).hasSize(1);

                Object event = saved.getDomainEvents().get(0);
                assertThat(event).isInstanceOf(MemberTerminatedEvent.class);

                MemberTerminatedEvent terminationEvent = (MemberTerminatedEvent) event;
                assertThat(terminationEvent.getMemberId()).isEqualTo(new UserId(testMemberId));
                assertThat(terminationEvent.getReason()).isEqualTo(DeactivationReason.OTHER);
                assertThat(terminationEvent.getTerminatedBy()).isEqualTo(new UserId(adminUserId));
                assertThat(terminationEvent.getNote()).isEqualTo("Administrative decision");
            }
        }

        @Nested
        @DisplayName("Already Terminated Tests")
        class AlreadyTerminatedTests {

            @Test
            @DisplayName("should reject termination of already terminated member")
            void shouldRejectTerminationOfAlreadyTerminatedMember() {
                Member terminatedMember = MemberTestDataBuilder.aMember()
                        .withId(testMemberId)
                        .withFirstName("Bob")
                        .withLastName("Jones")
                        .withRegistrationNumber("ZBM9999")
                        .withEmail("bob.jones@example.com")
                        .withPhone("+420111222333")
                        .withAddress(Address.of("Řeznická 1", "Brno", "60200", "CZ"))
                        .withNoGuardian()
                        .terminated(DeactivationReason.ODHLASKA, "Previous termination")
                        .build();

                when(memberRepository.findById(new UserId(testMemberId))).thenReturn(Optional.of(terminatedMember));

                var command = new Member.TerminateMembership(
                        new UserId(adminUserId), DeactivationReason.OTHER, "Second termination attempt"
                );
                assertThatThrownBy(() -> testedSubject.terminateMember(testMemberId, command))
                        .isInstanceOf(InvalidUpdateException.class)
                        .hasMessageContaining("Member is already terminated");

                verify(memberRepository, never()).save(any(Member.class));
            }
        }

        @Nested
        @DisplayName("Concurrent Termination Tests")
        class ConcurrentTerminationTests {

            @Test
            @DisplayName("should handle concurrent termination attempts with optimistic locking")
            void shouldHandleConcurrentTerminationAttempts() {
                when(memberRepository.findById(new UserId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class)))
                        .thenThrow(new OptimisticLockingFailureException("Concurrent modification detected"));

                var command = new Member.TerminateMembership(
                        new UserId(adminUserId), DeactivationReason.PRESTUP, null
                );
                assertThatThrownBy(() -> testedSubject.terminateMember(testMemberId, command))
                        .isInstanceOf(OptimisticLockingFailureException.class);

                verify(memberRepository).save(any(Member.class));
            }
        }

        @Nested
        @DisplayName("Authorization Tests")
        class AuthorizationTests {

            @Test
            @DisplayName("should reject termination by non-admin user")
            void shouldRejectTerminationByNonAdminUser() {
                UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken(
                        UUID.randomUUID().toString(), "password",
                        List.of(new SimpleGrantedAuthority("MEMBERS:READ"))
                );
                SecurityContextHolder.getContext().setAuthentication(userAuth);

                var command = new Member.TerminateMembership(
                        new UserId(adminUserId), DeactivationReason.ODHLASKA, null
                );
                assertThatThrownBy(() -> testedSubject.terminateMember(testMemberId, command))
                        .isInstanceOf(InvalidUpdateException.class)
                        .hasMessageContaining("Only users with MEMBERS:UPDATE permission");

                verify(memberRepository, never()).findById(any());
                verify(memberRepository, never()).save(any(Member.class));
            }

            @Test
            @DisplayName("should reject termination without authentication")
            void shouldRejectTerminationWithoutAuthentication() {
                SecurityContextHolder.clearContext();

                var command = new Member.TerminateMembership(
                        new UserId(adminUserId), DeactivationReason.ODHLASKA, null
                );
                assertThatThrownBy(() -> testedSubject.terminateMember(testMemberId, command))
                        .isInstanceOf(InvalidUpdateException.class)
                        .hasMessageContaining("User must be authenticated");

                verify(memberRepository, never()).findById(any());
                verify(memberRepository, never()).save(any(Member.class));
            }
        }

        @Nested
        @DisplayName("Member Not Found Tests")
        class MemberNotFoundTests {

            @Test
            @DisplayName("should throw exception when terminating non-existent member")
            void shouldThrowExceptionWhenTerminatingNonExistentMember() {
                UUID nonExistentId = UUID.randomUUID();

                when(memberRepository.findById(new UserId(nonExistentId))).thenReturn(Optional.empty());

                var command = new Member.TerminateMembership(
                        new UserId(adminUserId), DeactivationReason.ODHLASKA, null
                );
                assertThatThrownBy(() -> testedSubject.terminateMember(nonExistentId, command))
                        .isInstanceOf(InvalidUpdateException.class)
                        .hasMessageContaining("Member not found");

                verify(memberRepository, never()).save(any(Member.class));
            }
        }
    }
}
