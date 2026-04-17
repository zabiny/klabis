package com.klabis.members.application;

import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.*;
import com.klabis.members.domain.*;
import com.klabis.groups.LastOwnershipChecker;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ManagementPort Unit Tests")
class ManagementServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private LastOwnershipChecker lastOwnershipChecker;

    private ManagementPort testedSubject;
    private UUID testMemberId;
    private Member testMember;

    @BeforeEach
    void setUp() {
        testedSubject = new ManagementService(memberRepository, userService, lastOwnershipChecker, eventPublisher);

        testMemberId = UUID.randomUUID();
        testMember = MemberTestDataBuilder.aMember()
                .withId(testMemberId)
                .withFirstName("John")
                .withLastName("Doe")
                .withRegistrationNumber("ZBM1234")
                .withDateOfBirth(java.time.LocalDate.of(1990, 5, 15))
                .withNationality("SK")
                .withEmail("john.doe@example.com")
                .withPhone("+420123456789")
                .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                .withNoGuardian()
                .build();
    }

    @Nested
    @DisplayName("Member Update Tests")
    class MemberUpdateTests {

        @Test
        @DisplayName("should update member birth number via update command")
        void shouldUpdateMemberWithBirthNumber() {
            var command = MemberUpdateMemberBuilder.builder()
                    .bankAccountNumber(BankAccountNumber.of("12345/5678"))
                    .birthNumber(BirthNumber.of("900101/1234"))
                    .build();

            Member czechMember = MemberTestDataBuilder.aMember()
                    .withId(testMemberId)
                    .withNationality("CZ")
                    .withBirthNumber("900515/1234")
                    .build();
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(czechMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            Member result = testedSubject.updateMember(new MemberId(testMemberId), command);

            assertThat(result.getId().uuid()).isEqualTo(testMemberId);
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("should update member bank account number via update command")
        void shouldUpdateMemberWithBankAccountNumber() {
            var command = MemberUpdateMemberBuilder.builder()
                    .bankAccountNumber(BankAccountNumber.of("12345/5678"))
                    .build();

            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            Member result = testedSubject.updateMember(new MemberId(testMemberId), command);

            assertThat(result.getId().uuid()).isEqualTo(testMemberId);
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("should update member contact info via update command")
        void shouldUpdateMemberContactInfo() {
            var command = MemberUpdateMemberBuilder.builder()
                    .email(EmailAddress.of("new@example.com"))
                    .build();

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

            @BeforeEach
            void setUpNested() {
                when(lastOwnershipChecker.findGroupsOwnedSolely(any())).thenReturn(List.of());
            }

            @Test
            @DisplayName("should suspend active member with ODHLASKA reason")
            void shouldSuspendActiveMemberWithOdhlaskaReason() {
                when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

                var command = MemberSuspendMembershipBuilder.builder()
                        .suspendedBy(new UserId(adminUserId))
                        .reason(DeactivationReason.ODHLASKA)
                        .note("Member requested resignation")
                        .build();
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

                var command = MemberSuspendMembershipBuilder.builder()
                        .suspendedBy(new UserId(adminUserId))
                        .reason(DeactivationReason.ODHLASKA)
                        .note("Member requested resignation")
                        .build();
                Member result = testedSubject.suspendMember(new MemberId(testMemberId), command);

                assertThat(result.isActive()).isFalse();
                verify(userService).suspendUser(testActiveMember.getId().toUserId());
            }

            @Test
            @DisplayName("should suspend active member without note")
            void shouldSuspendActiveMemberWithoutNote() {
                when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

                var command = MemberSuspendMembershipBuilder.builder()
                        .suspendedBy(new UserId(adminUserId))
                        .reason(DeactivationReason.PRESTUP)
                        .note(null)
                        .build();
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

                var command = MemberSuspendMembershipBuilder.builder()
                        .suspendedBy(new UserId(adminUserId))
                        .reason(DeactivationReason.OTHER)
                        .note("Administrative decision")
                        .build();
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
        @DisplayName("Last Group Owner Tests")
        class LastGroupOwnerTests {

            @Test
            @DisplayName("should throw MemberIsLastGroupOwnerException when member is the sole owner of a group")
            void shouldThrowWhenMemberIsLastGroupOwner() {
                var ownedGroups = List.of(
                        new LastOwnershipChecker.OwnedGroupInfo("group-id-1", "Trail Runners", "FREE"),
                        new LastOwnershipChecker.OwnedGroupInfo("group-id-2", "Juniors", "TRAINING")
                );
                when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(lastOwnershipChecker.findGroupsOwnedSolely(new MemberId(testMemberId))).thenReturn(ownedGroups);

                var command = MemberSuspendMembershipBuilder.builder()
                        .suspendedBy(new UserId(adminUserId))
                        .reason(DeactivationReason.ODHLASKA)
                        .note(null)
                        .build();

                assertThatThrownBy(() -> testedSubject.suspendMember(new MemberId(testMemberId), command))
                        .isInstanceOf(MemberIsLastGroupOwnerException.class)
                        .satisfies(ex -> {
                            var e = (MemberIsLastGroupOwnerException) ex;
                            assertThat(e.getGroups()).hasSize(2);
                            assertThat(e.getGroups()).extracting(LastOwnershipChecker.OwnedGroupInfo::groupName)
                                    .containsExactlyInAnyOrder("Trail Runners", "Juniors");
                        });

                verify(memberRepository, never()).save(any(Member.class));
                verify(userService, never()).suspendUser(any(UserId.class));
            }

            @Test
            @DisplayName("should proceed with suspension when member is not the sole owner of any group")
            void shouldProceedWhenMemberIsNotLastOwnerOfAnyGroup() {
                when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(lastOwnershipChecker.findGroupsOwnedSolely(new MemberId(testMemberId))).thenReturn(List.of());
                when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

                var command = MemberSuspendMembershipBuilder.builder()
                        .suspendedBy(new UserId(adminUserId))
                        .reason(DeactivationReason.ODHLASKA)
                        .note(null)
                        .build();

                Member result = testedSubject.suspendMember(new MemberId(testMemberId), command);

                assertThat(result.isActive()).isFalse();
                verify(memberRepository).save(any(Member.class));
                verify(userService).suspendUser(testActiveMember.getId().toUserId());
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

                var command = MemberSuspendMembershipBuilder.builder()
                        .suspendedBy(new UserId(adminUserId))
                        .reason(DeactivationReason.OTHER)
                        .note("Second termination attempt")
                        .build();
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

                var command = MemberSuspendMembershipBuilder.builder()
                        .suspendedBy(new UserId(adminUserId))
                        .reason(DeactivationReason.PRESTUP)
                        .note(null)
                        .build();
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

                var command = MemberSuspendMembershipBuilder.builder()
                        .suspendedBy(new UserId(adminUserId))
                        .reason(DeactivationReason.ODHLASKA)
                        .note(null)
                        .build();
                assertThatThrownBy(() -> testedSubject.suspendMember(new MemberId(nonExistentId), command))
                        .isInstanceOf(MemberNotFoundException.class)
                        .hasMessageContaining("Member not found");

                verify(memberRepository, never()).save(any(Member.class));
            }
        }
    }

    @Nested
    @DisplayName("Get Member And Record View Tests")
    class GetMemberAndRecordViewTests {

        private UUID viewerUserId;

        @BeforeEach
        void setUp() {
            viewerUserId = UUID.randomUUID();
        }

        @Test
        @DisplayName("should return active member for user without MANAGE authority")
        void shouldReturnActiveMemberForNonAdmin() {
            Member activeMember = MemberTestDataBuilder.aMember().withId(testMemberId).withActive(true).build();
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(activeMember));

            Member result = testedSubject.getMemberAndRecordView(new MemberId(testMemberId), new UserId(viewerUserId), false);

            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("should throw MemberNotFoundException for inactive member when caller lacks MANAGE authority")
        void shouldThrow404ForInactiveMemberWithoutManage() {
            Member inactiveMember = MemberTestDataBuilder.aMember().withId(testMemberId).withActive(false).build();
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(inactiveMember));

            assertThatThrownBy(() -> testedSubject.getMemberAndRecordView(new MemberId(testMemberId), new UserId(viewerUserId), false))
                    .isInstanceOf(MemberNotFoundException.class);

            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("should return inactive member for user with MANAGE authority")
        void shouldReturnInactiveMemberForAdmin() {
            Member inactiveMember = MemberTestDataBuilder.aMember().withId(testMemberId).withActive(false).build();
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(inactiveMember));

            Member result = testedSubject.getMemberAndRecordView(new MemberId(testMemberId), new UserId(viewerUserId), true);

            assertThat(result.isActive()).isFalse();
        }

        @Test
        @DisplayName("should throw MemberNotFoundException when member does not exist")
        void shouldThrowWhenMemberNotFound() {
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testedSubject.getMemberAndRecordView(new MemberId(testMemberId), new UserId(viewerUserId), false))
                    .isInstanceOf(MemberNotFoundException.class);
        }

        @Test
        @DisplayName("should publish BirthNumberAccessedEvent when admin views member with birth number")
        void shouldPublishBirthNumberAccessedEventForAdmin() {
            Member memberWithBirthNumber = MemberTestDataBuilder.aMember()
                    .withId(testMemberId)
                    .withNationality("CZ")
                    .withBirthNumber("900101/1234")
                    .build();
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(memberWithBirthNumber));

            testedSubject.getMemberAndRecordView(new MemberId(testMemberId), new UserId(viewerUserId), true);

            verify(eventPublisher).publishEvent(any(BirthNumberAccessedEvent.class));
        }

        @Test
        @DisplayName("should not publish BirthNumberAccessedEvent when member has no birth number")
        void shouldNotPublishEventWhenNoBirthNumber() {
            Member memberWithoutBirthNumber = MemberTestDataBuilder.aMember()
                    .withId(testMemberId)
                    .withNationality("SK")
                    .build();
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(memberWithoutBirthNumber));

            testedSubject.getMemberAndRecordView(new MemberId(testMemberId), new UserId(viewerUserId), true);

            verify(eventPublisher, never()).publishEvent(any(BirthNumberAccessedEvent.class));
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

            var command = MemberResumeMembershipBuilder.builder().resumedBy(new UserId(adminUserId)).build();
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

            var command = MemberResumeMembershipBuilder.builder().resumedBy(new UserId(adminUserId)).build();
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

            var command = MemberResumeMembershipBuilder.builder().resumedBy(new UserId(adminUserId)).build();
            Member result = testedSubject.resumeMember(new MemberId(testMemberId), command);

            assertThat(result.isActive()).isTrue();
            verify(userService).resumeUser(testSuspendedMember.getId().toUserId());
        }

        @Test
        @DisplayName("should publish MemberResumedEvent on successful resume")
        void shouldPublishMemberResumedEventOnSuccessfulResume() {
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testSuspendedMember));
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = MemberResumeMembershipBuilder.builder().resumedBy(new UserId(adminUserId)).build();
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
