package com.klabis.members.application;

import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.MemberId;
import com.klabis.members.MemberResumedEvent;
import com.klabis.members.MemberSuspendedEvent;
import com.klabis.members.MemberTestDataBuilder;
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
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            Member result = testedSubject.updateMember(new MemberId(testMemberId), command);

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

            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            Member result = testedSubject.updateMember(new MemberId(testMemberId), command);

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

            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            Member result = testedSubject.updateMember(new MemberId(testMemberId), command);

            assertThat(result.getId().uuid()).isEqualTo(testMemberId);
            verify(memberRepository).save(any(Member.class));
        }
    }

    @Nested
    @DisplayName("Member Suspension Tests")
    class MemberSuspensionTests {

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
        }

        @Nested
        @DisplayName("Successful Suspension Tests")
        class SuccessfulSuspensionTests {

            @Test
            @DisplayName("should suspend active member with ODHLASKA reason")
            void shouldSuspendActiveMemberWithOdhlaskaReason() {
                when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

                var command = new Member.SuspendMembership(
                        new UserId(adminUserId), DeactivationReason.ODHLASKA, "Member requested resignation"
                );
                Member result = testedSubject.suspendMember(new MemberId(testMemberId), command);

                assertThat(result.getId().uuid()).isEqualTo(testMemberId);

                ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
                verify(memberRepository).save(captor.capture());

                Member saved = captor.getValue();
                assertThat(saved.isActive()).isFalse();
                assertThat(saved.getSuspensionReason()).isEqualTo(DeactivationReason.ODHLASKA);
                assertThat(saved.getSuspendedAt()).isNotNull();
                assertThat(saved.getSuspensionNote()).isEqualTo("Member requested resignation");
                assertThat(saved.getSuspendedBy().uuid()).isEqualTo(adminUserId);

                verify(userService).suspendUser(testActiveMember.getId().toUserId());
            }

            @Test
            @DisplayName("should handle missing User account gracefully during suspension")
            void shouldHandleMissingUserAccountGracefully() {
                when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
                doNothing().when(userService).suspendUser(any(UserId.class));

                var command = new Member.SuspendMembership(
                        new UserId(adminUserId), DeactivationReason.ODHLASKA, "Member requested resignation"
                );
                Member result = testedSubject.suspendMember(new MemberId(testMemberId), command);

                assertThat(result.isActive()).isFalse();
                verify(userService).suspendUser(testActiveMember.getId().toUserId());
            }

            @Test
            @DisplayName("should suspend active member without note")
            void shouldSuspendActiveMemberWithoutNote() {
                when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

                var command = new Member.SuspendMembership(
                        new UserId(adminUserId), DeactivationReason.PRESTUP, null
                );
                Member result = testedSubject.suspendMember(new MemberId(testMemberId), command);

                assertThat(result.getId().uuid()).isEqualTo(testMemberId);

                ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
                verify(memberRepository).save(captor.capture());

                assertThat(captor.getValue().isActive()).isFalse();
                assertThat(captor.getValue().getSuspensionReason()).isEqualTo(DeactivationReason.PRESTUP);
                assertThat(captor.getValue().getSuspensionNote()).isNull();
            }

            @Test
            @DisplayName("should publish MemberSuspendedEvent on successful suspension")
            void shouldPublishMemberSuspendedEventOnSuccessfulSuspension() {
                when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

                var command = new Member.SuspendMembership(
                        new UserId(adminUserId), DeactivationReason.OTHER, "Administrative decision"
                );
                testedSubject.suspendMember(new MemberId(testMemberId), command);

                ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
                verify(memberRepository).save(captor.capture());

                Member saved = captor.getValue();
                assertThat(saved.getDomainEvents()).hasSize(1);

                Object event = saved.getDomainEvents().get(0);
                assertThat(event).isInstanceOf(MemberSuspendedEvent.class);

                MemberSuspendedEvent suspensionEvent = (MemberSuspendedEvent) event;
                assertThat(suspensionEvent.memberId()).isEqualTo(new MemberId(testMemberId));
                assertThat(suspensionEvent.reason()).isEqualTo(DeactivationReason.OTHER);
                assertThat(suspensionEvent.suspendedBy()).isEqualTo(new UserId(adminUserId));
                assertThat(suspensionEvent.note()).isEqualTo("Administrative decision");
            }
        }

        @Nested
        @DisplayName("Already Suspended Tests")
        class AlreadySuspendedTests {

            @Test
            @DisplayName("should reject suspension of already suspended member")
            void shouldRejectSuspensionOfAlreadySuspendedMember() {
                Member suspendedMember = MemberTestDataBuilder.aMember()
                        .withId(testMemberId)
                        .withFirstName("Bob")
                        .withLastName("Jones")
                        .withRegistrationNumber("ZBM9999")
                        .withEmail("bob.jones@example.com")
                        .withPhone("+420111222333")
                        .withAddress(Address.of("Řeznická 1", "Brno", "60200", "CZ"))
                        .withNoGuardian()
                        .suspended(DeactivationReason.ODHLASKA, "Previous termination")
                        .build();

                when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(suspendedMember));

                var command = new Member.SuspendMembership(
                        new UserId(adminUserId), DeactivationReason.OTHER, "Second termination attempt"
                );
                assertThatThrownBy(() -> testedSubject.suspendMember(new MemberId(testMemberId), command))
                        .isInstanceOf(InvalidUpdateException.class)
                        .hasMessageContaining("Member is already suspended");

                verify(memberRepository, never()).save(any(Member.class));
            }
        }

        @Nested
        @DisplayName("Concurrent Suspension Tests")
        class ConcurrentSuspensionTests {

            @Test
            @DisplayName("should handle concurrent suspension attempts with optimistic locking")
            void shouldHandleConcurrentSuspensionAttempts() {
                when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class)))
                        .thenThrow(new OptimisticLockingFailureException("Concurrent modification detected"));

                var command = new Member.SuspendMembership(
                        new UserId(adminUserId), DeactivationReason.PRESTUP, null
                );
                assertThatThrownBy(() -> testedSubject.suspendMember(new MemberId(testMemberId), command))
                        .isInstanceOf(OptimisticLockingFailureException.class);

                verify(memberRepository).save(any(Member.class));
            }
        }

        @Nested
        @DisplayName("Member Not Found Tests")
        class MemberNotFoundTests {

            @Test
            @DisplayName("should throw exception when suspending non-existent member")
            void shouldThrowExceptionWhenSuspendingNonExistentMember() {
                UUID nonExistentId = UUID.randomUUID();

                when(memberRepository.findById(new MemberId(nonExistentId))).thenReturn(Optional.empty());

                var command = new Member.SuspendMembership(
                        new UserId(adminUserId), DeactivationReason.ODHLASKA, null
                );
                assertThatThrownBy(() -> testedSubject.suspendMember(new MemberId(nonExistentId), command))
                        .isInstanceOf(InvalidUpdateException.class)
                        .hasMessageContaining("Member not found");

                verify(memberRepository, never()).save(any(Member.class));
            }
        }
    }

    @Nested
    @DisplayName("Member Resume Tests")
    class MemberResumeTests {

        private Member testSuspendedMember;
        private UUID adminUserId;

        @BeforeEach
        void setUpNested() {
            adminUserId = UUID.randomUUID();

            testSuspendedMember = MemberTestDataBuilder.aMember()
                    .withId(testMemberId)
                    .withFirstName("Jane")
                    .withLastName("Smith")
                    .withRegistrationNumber("ZBM9998")
                    .withEmail("jane.smith@example.com")
                    .withPhone("+420987654321")
                    .withAddress(Address.of("Reakční 10", "Brno", "60200", "CZ"))
                    .withNoGuardian()
                    .suspended(DeactivationReason.ODHLASKA, "Previous termination")
                    .build();
        }

        @Test
        @DisplayName("should resume suspended member successfully")
        void shouldResumeSuspendedMemberSuccessfully() {
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testSuspendedMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new Member.ResumeMembership(new UserId(adminUserId));
            Member result = testedSubject.resumeMember(new MemberId(testMemberId), command);

            assertThat(result.isActive()).isTrue();
            verify(userService).resumeUser(testSuspendedMember.getId().toUserId());
        }

        @Test
        @DisplayName("should reject resume of already active member")
        void shouldRejectResumeOfAlreadyActiveMember() {
            Member activeMember = MemberTestDataBuilder.aMember()
                    .withId(testMemberId)
                    .withFirstName("Bob")
                    .withLastName("Jones")
                    .withRegistrationNumber("ZBM9999")
                    .withEmail("bob.jones@example.com")
                    .withPhone("+420111222333")
                    .withAddress(Address.of("Řeznická 1", "Brno", "60200", "CZ"))
                    .withNoGuardian()
                    .build();

            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(activeMember));

            var command = new Member.ResumeMembership(new UserId(adminUserId));
            assertThatThrownBy(() -> testedSubject.resumeMember(new MemberId(testMemberId), command))
                    .isInstanceOf(InvalidUpdateException.class)
                    .hasMessageContaining("already active");

            verify(memberRepository, never()).save(any(Member.class));
            verify(userService, never()).resumeUser(any(UserId.class));
        }

        @Test
        @DisplayName("should handle missing User account gracefully during resume")
        void shouldHandleMissingUserAccountGracefully() {
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testSuspendedMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(userService).resumeUser(any(UserId.class));

            var command = new Member.ResumeMembership(new UserId(adminUserId));
            Member result = testedSubject.resumeMember(new MemberId(testMemberId), command);

            assertThat(result.isActive()).isTrue();
            verify(userService).resumeUser(testSuspendedMember.getId().toUserId());
        }

        @Test
        @DisplayName("should publish MemberResumedEvent on successful resume")
        void shouldPublishMemberResumedEventOnSuccessfulResume() {
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testSuspendedMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = new Member.ResumeMembership(new UserId(adminUserId));
            testedSubject.resumeMember(new MemberId(testMemberId), command);

            ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(captor.capture());

            Member saved = captor.getValue();
            assertThat(saved.getDomainEvents()).hasSize(1);

            Object event = saved.getDomainEvents().get(0);
            assertThat(event).isInstanceOf(MemberResumedEvent.class);

            MemberResumedEvent resumeEvent = (MemberResumedEvent) event;
            assertThat(resumeEvent.memberId()).isEqualTo(new MemberId(testMemberId));
            assertThat(resumeEvent.resumedBy()).isEqualTo(new UserId(adminUserId));
            assertThat(resumeEvent.registrationNumber()).isEqualTo(testSuspendedMember.getRegistrationNumber());
        }
    }
}
