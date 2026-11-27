package com.klabis.members.application;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.exceptions.InsufficientAuthorityException;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.*;
import com.klabis.members.domain.*;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManagementPort Unit Tests")
class ManagementServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final MemberFinancialStatePort memberFinancialStatePort = mock(MemberFinancialStatePort.class,
            invocation -> new MemberFinancialStatePort.MemberFinancialSnapshot(
                    invocation.getArgument(0), new MonetaryAmount(BigDecimal.ZERO, "CZK"), false));

    @Mock
    private MemberOwnedGroupsPort memberOwnedGroupsPort;

    @Mock
    private MemberCompletenessPort memberCompletenessPort;

    @Mock
    private LegalGuardianCascadePort legalGuardianCascadePort;

    @Mock
    private AuthorizationEvaluator authorizationEvaluator;

    private ManagementPort testedSubject;
    private UUID testMemberId;
    private Member testMember;

    @BeforeEach
    void setUp() {
        testedSubject = new ManagementService(memberRepository, userService, eventPublisher, Optional.of(memberFinancialStatePort), List.of(memberOwnedGroupsPort), memberCompletenessPort, legalGuardianCascadePort, authorizationEvaluator);

        testMemberId = UUID.randomUUID();
        testMember = MemberTestDataBuilder.aMember()
                .withId(testMemberId)
                .withFirstName("John")
                .withLastName("Doe")
                .withRegistrationNumber("ZBM1234")
                .withDateOfBirth(LocalDate.of(1990, 5, 15))
                .withNationality("SK")
                .withEmail("john.doe@example.com")
                .withPhone("+420123456789")
                .withAddress(Address.of("Hlavní 123", "Praha", "11000", Country.of("CZ")))
                .build();
    }

    @Nested
    @DisplayName("Member Update Tests")
    class MemberUpdateTests {

        @Test
        @DisplayName("should apply the command to the loaded member, pass the guardian contacts to the domain and save it")
        void shouldUpdateMemberAndPassGuardianContactsToDomain() {
            Member minor = MemberTestDataBuilder.aMember()
                    .withId(testMemberId)
                    .withDateOfBirth(LocalDate.now().minusYears(10))
                    .withNationality("SK")
                    .withEmail("anna@example.com")
                    .withPhone("+420123456789")
                    .build();
            var command = MemberUpdateMemberBuilder.builder(Member.UpdateMember.from(minor))
                    .email(null)
                    .bankAccountNumber(BankAccountNumber.of("12345/5678"))
                    .build();
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(minor));
            when(memberCompletenessPort.guardianContactsOf(minor)).thenReturn(new GuardianContacts(true, true, true));
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            Member result = testedSubject.updateMember(new MemberId(testMemberId), command);

            ArgumentCaptor<Member> saved = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(saved.capture());
            assertThat(saved.getValue()).isSameAs(minor);
            assertThat(result.getEmail()).isNull();
            assertThat(result.getBankAccountNumber()).isEqualTo(BankAccountNumber.of("12345/5678"));
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
                    .withAddress(Address.of("Vinohradská 456", "Praha", "12000", Country.of("CZ")))
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

                var command = MemberSuspendMembershipBuilder.builder()
                        .suspendedBy(new UserId(adminUserId))
                        .reason(DeactivationReason.ODHLASKA)
                        .note("Member requested resignation")
                        .build();
                Member result = testedSubject.suspendMember(new MemberId(testMemberId), command, Set.of());

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

        }

        @Nested
        @DisplayName("Last Group Owner Tests")
        class LastGroupOwnerTests {

            @Test
            @DisplayName("should throw SuspensionBlockedException when member is the sole owner of a group")
            void shouldThrowWhenMemberIsLastGroupOwner() {
                when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(memberOwnedGroupsPort.findGroupsBlockingSuspension(new MemberId(testMemberId))).thenReturn(List.of(
                        new OwnedGroup("group-id-1", "Trail Runners", "FREE", java.net.URI.create("/api/groups/group-id-1")),
                        new OwnedGroup("group-id-2", "Juniors", "TRAINING", java.net.URI.create("/api/groups/group-id-2"))));

                var command = MemberSuspendMembershipBuilder.builder()
                        .suspendedBy(new UserId(adminUserId))
                        .reason(DeactivationReason.ODHLASKA)
                        .note(null)
                        .build();

                assertThatThrownBy(() -> testedSubject.suspendMember(new MemberId(testMemberId), command, Set.of()))
                        .isInstanceOf(SuspensionBlockedException.class)
                        .satisfies(ex -> {
                            var e = (SuspensionBlockedException) ex;
                            assertThat(e.getBlockingGroups()).hasSize(2);
                            assertThat(e.getBlockingGroups()).extracting(OwnedGroup::groupName)
                                    .containsExactlyInAnyOrder("Trail Runners", "Juniors");
                            assertThat(e.getDebtSnapshot()).isNull();
                        });

                verify(memberRepository, never()).save(any(Member.class));
                verify(userService, never()).suspendUser(any(UserId.class));
            }

        }

        @Nested
        @DisplayName("Outstanding Debt Pre-Check Tests")
        class OutstandingDebtPreCheckTests {

            @Test
            @DisplayName("should throw SuspensionBlockedException when member has negative balance")
            void shouldThrowWhenMemberHasNegativeBalance() {
                when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                var snapshot = new MemberFinancialStatePort.MemberFinancialSnapshot(
                        new MemberId(testMemberId),
                        new MonetaryAmount(new BigDecimal("-250"), "CZK"),
                        true);
                when(memberFinancialStatePort.getFinancialSnapshot(new MemberId(testMemberId))).thenReturn(snapshot);

                var command = MemberSuspendMembershipBuilder.builder()
                        .suspendedBy(new UserId(adminUserId))
                        .reason(DeactivationReason.ODHLASKA)
                        .note(null)
                        .build();

                assertThatThrownBy(() -> testedSubject.suspendMember(new MemberId(testMemberId), command, Set.of()))
                        .isInstanceOf(SuspensionBlockedException.class)
                        .satisfies(ex -> {
                            var e = (SuspensionBlockedException) ex;
                            assertThat(e.getDebtSnapshot().hasOutstandingDebt()).isTrue();
                            assertThat(e.getDebtSnapshot().balance().amount()).isEqualByComparingTo(new BigDecimal("-250"));
                            assertThat(e.getBlockingGroups()).isEmpty();
                        });

                verify(memberRepository, never()).save(any(Member.class));
                verify(userService, never()).suspendUser(any(UserId.class));
            }

            @Test
            @DisplayName("should report both blockers when member is last group owner AND has outstanding debt")
            void shouldReportBothBlockersWhenBothConditionsApply() {
                when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testActiveMember));
                when(memberOwnedGroupsPort.findGroupsBlockingSuspension(new MemberId(testMemberId))).thenReturn(List.of(
                        new OwnedGroup("group-id-1", "Trail Runners", "FREE", java.net.URI.create("/api/groups/group-id-1"))));
                var snapshot = new MemberFinancialStatePort.MemberFinancialSnapshot(
                        new MemberId(testMemberId),
                        new MonetaryAmount(new BigDecimal("-250"), "CZK"),
                        true);
                when(memberFinancialStatePort.getFinancialSnapshot(new MemberId(testMemberId))).thenReturn(snapshot);

                var command = MemberSuspendMembershipBuilder.builder()
                        .suspendedBy(new UserId(adminUserId))
                        .reason(DeactivationReason.ODHLASKA)
                        .note(null)
                        .build();

                assertThatThrownBy(() -> testedSubject.suspendMember(new MemberId(testMemberId), command, Set.of()))
                        .isInstanceOf(SuspensionBlockedException.class)
                        .satisfies(ex -> {
                            var e = (SuspensionBlockedException) ex;
                            assertThat(e.getBlockingGroups()).hasSize(1);
                            assertThat(e.getBlockingGroups()).extracting(OwnedGroup::groupName)
                                    .containsExactly("Trail Runners");
                            assertThat(e.getDebtSnapshot()).isNotNull();
                            assertThat(e.getDebtSnapshot().hasOutstandingDebt()).isTrue();
                            assertThat(e.getDebtSnapshot().balance().amount()).isEqualByComparingTo(new BigDecimal("-250"));
                        });

                verify(memberRepository, never()).save(any(Member.class));
                verify(userService, never()).suspendUser(any(UserId.class));
            }
        }

        @Nested
        @DisplayName("Whole Legal Guardian Group Cascade Tests")
        class WholeGroupCascadeTests {

            private final LegalGuardianGroupId groupId = new LegalGuardianGroupId(UUID.randomUUID());
            private MemberId guardianId;
            private MemberId minorOneId;
            private MemberId minorTwoId;
            private Member minorOne;
            private Member minorTwo;
            private Member.SuspendMembership command;

            @BeforeEach
            void setUpCascade() {
                guardianId = new MemberId(testMemberId);
                minorOneId = new MemberId(UUID.randomUUID());
                minorTwoId = new MemberId(UUID.randomUUID());
                minorOne = aMinor(minorOneId, "Anna");
                minorTwo = aMinor(minorTwoId, "Berta");
                command = MemberSuspendMembershipBuilder.builder()
                        .suspendedBy(new UserId(adminUserId))
                        .reason(DeactivationReason.OTHER)
                        .note("Family left the club")
                        .build();
            }

            private void givenFamilyLoadable() {
                when(memberRepository.findById(guardianId)).thenReturn(Optional.of(testActiveMember));
                when(memberRepository.findById(minorOneId)).thenReturn(Optional.of(minorOne));
                when(memberRepository.findById(minorTwoId)).thenReturn(Optional.of(minorTwo));
                when(legalGuardianCascadePort.minorsOfSoleGuardianGroups(guardianId, Set.of(groupId)))
                        .thenReturn(List.of(minorOneId, minorTwoId));
                when(authorizationEvaluator.has(eq(Authority.MEMBERS_MANAGE), any(TargetRef.class))).thenReturn(true);
            }

            private void givenSaveReturnsSavedMember() {
                when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
            }

            private void givenGuardianOwnsOnlyTheCascadedGroup() {
                when(memberOwnedGroupsPort.findGroupsBlockingSuspension(guardianId)).thenReturn(List.of(guardianGroup()));
            }

            private Member aMinor(MemberId id, String firstName) {
                return MemberTestDataBuilder.aMember()
                        .withId(id.uuid())
                        .withFirstName(firstName)
                        .withLastName("Smith")
                        .withRegistrationNumber("ZBM" + (1000 + Math.abs(id.uuid().hashCode() % 8000)))
                        .withDateOfBirth(LocalDate.now().minusYears(10))
                        .build();
            }

            private OwnedGroup guardianGroup() {
                return new OwnedGroup(groupId.uuid().toString(), "Smith", "LEGAL_GUARDIAN",
                        java.net.URI.create("/api/legal-guardian-groups/" + groupId.uuid()));
            }

            private void minorOwesMoney(MemberId minorId) {
                when(memberFinancialStatePort.getFinancialSnapshot(minorId)).thenReturn(
                        new MemberFinancialStatePort.MemberFinancialSnapshot(minorId,
                                new MonetaryAmount(new BigDecimal("-100"), "CZK"), true));
            }

            private void assertNobodySuspended() {
                verify(memberRepository, never()).save(any(Member.class));
                verify(userService, never()).suspendUser(any(UserId.class));
            }

            @Test
            @DisplayName("should suspend guardian and all minors with the same reason and note")
            void shouldSuspendGuardianAndMinorsWithSameReasonAndNote() {
                givenFamilyLoadable();
                givenSaveReturnsSavedMember();
                givenGuardianOwnsOnlyTheCascadedGroup();
                testedSubject.suspendMember(guardianId, command, Set.of(groupId));

                assertThat(List.of(testActiveMember, minorOne, minorTwo)).allSatisfy(member -> {
                    assertThat(member.isActive()).isFalse();
                    assertThat(member.getSuspensionReason()).isEqualTo(DeactivationReason.OTHER);
                    assertThat(member.getSuspensionNote()).isEqualTo("Family left the club");
                    assertThat(member.getSuspendedBy().uuid()).isEqualTo(adminUserId);
                    assertThat(member.getDomainEvents()).hasSize(1).first().isInstanceOf(MemberSuspendedEvent.class);
                });
                verify(userService).suspendUser(guardianId.toUserId());
                verify(userService).suspendUser(minorOneId.toUserId());
                verify(userService).suspendUser(minorTwoId.toUserId());
            }

            @Test
            @DisplayName("should block everybody and attribute the debt to the minor")
            void shouldBlockEverybodyWhenMinorHasDebt() {
                givenFamilyLoadable();
                givenGuardianOwnsOnlyTheCascadedGroup();
                minorOwesMoney(minorTwoId);

                assertThatThrownBy(() -> testedSubject.suspendMember(guardianId, command, Set.of(groupId)))
                        .isInstanceOfSatisfying(SuspensionBlockedException.class, e -> {
                            assertThat(e.getDebtSnapshot()).isNull();
                            assertThat(e.getBlockingGroups()).isEmpty();
                            assertThat(e.getDependents()).singleElement().satisfies(dependent -> {
                                assertThat(dependent.memberId()).isEqualTo(minorTwoId);
                                assertThat(dependent.memberName()).isEqualTo("Berta Smith");
                                assertThat(dependent.debtSnapshot().hasOutstandingDebt()).isTrue();
                                assertThat(dependent.blockingGroups()).isEmpty();
                            });
                        });

                assertNobodySuspended();
            }

            @Test
            @DisplayName("should block everybody when a minor is the last owner of a group")
            void shouldBlockEverybodyWhenMinorIsLastOwner() {
                givenFamilyLoadable();
                givenGuardianOwnsOnlyTheCascadedGroup();
                var freeGroup = new OwnedGroup("free-1", "Kids", "FREE", java.net.URI.create("/api/groups/free-1"));
                when(memberOwnedGroupsPort.findGroupsBlockingSuspension(minorOneId)).thenReturn(List.of(freeGroup));

                assertThatThrownBy(() -> testedSubject.suspendMember(guardianId, command, Set.of(groupId)))
                        .isInstanceOfSatisfying(SuspensionBlockedException.class, e ->
                                assertThat(e.getDependents()).singleElement().satisfies(dependent -> {
                                    assertThat(dependent.memberId()).isEqualTo(minorOneId);
                                    assertThat(dependent.blockingGroups()).containsExactly(freeGroup);
                                }));

                assertNobodySuspended();
            }

            @Test
            @DisplayName("should still report other blockers of the guardian together with the dependents' blockers")
            void shouldReportGuardiansOtherBlockers() {
                givenFamilyLoadable();
                var trainingGroup = new OwnedGroup("training-1", "Juniors", "TRAINING",
                        java.net.URI.create("/api/training-groups/training-1"));
                when(memberOwnedGroupsPort.findGroupsBlockingSuspension(guardianId))
                        .thenReturn(List.of(guardianGroup(), trainingGroup));
                minorOwesMoney(minorOneId);

                assertThatThrownBy(() -> testedSubject.suspendMember(guardianId, command, Set.of(groupId)))
                        .isInstanceOfSatisfying(SuspensionBlockedException.class, e -> {
                            assertThat(e.getBlockingGroups()).containsExactly(trainingGroup);
                            assertThat(e.getDependents()).extracting(SuspensionBlockedException.DependentBlockers::memberId)
                                    .containsExactly(minorOneId);
                        });

                assertNobodySuspended();
            }

            @Test
            @DisplayName("should propagate the rejection of an id that is not a sole guardian group of the member")
            void shouldRejectGroupWhereMemberIsNotSoleGuardian() {
                when(memberRepository.findById(guardianId)).thenReturn(Optional.of(testActiveMember));
                when(legalGuardianCascadePort.minorsOfSoleGuardianGroups(guardianId, Set.of(groupId)))
                        .thenThrow(new NotSoleGuardianGroupException(groupId));

                assertThatThrownBy(() -> testedSubject.suspendMember(guardianId, command, Set.of(groupId)))
                        .isInstanceOf(NotSoleGuardianGroupException.class);

                assertNobodySuspended();
            }

            @Test
            @DisplayName("should skip an already suspended minor and keep its original suspension")
            void shouldSkipAlreadySuspendedMinor() {
                givenFamilyLoadable();
                givenSaveReturnsSavedMember();
                givenGuardianOwnsOnlyTheCascadedGroup();
                minorOne.suspend(new Member.SuspendMembership(new UserId(UUID.randomUUID()), DeactivationReason.ODHLASKA, "Earlier"));
                minorOne.clearDomainEvents();

                testedSubject.suspendMember(guardianId, command, Set.of(groupId));

                assertThat(minorOne.getSuspensionReason()).isEqualTo(DeactivationReason.ODHLASKA);
                assertThat(minorOne.getSuspensionNote()).isEqualTo("Earlier");
                assertThat(minorOne.getDomainEvents()).isEmpty();
                assertThat(minorTwo.isActive()).isFalse();
                assertThat(testActiveMember.isActive()).isFalse();
                verify(userService, never()).suspendUser(minorOneId.toUserId());
                verify(memberFinancialStatePort, never()).getFinancialSnapshot(minorOneId);
            }

            @Test
            @DisplayName("should fail as a whole when suspension of a later member fails")
            void shouldFailWhenSuspensionOfLaterMemberFails() {
                givenFamilyLoadable();
                givenGuardianOwnsOnlyTheCascadedGroup();
                givenSaveReturnsSavedMember();
                when(memberRepository.save(minorTwo)).thenThrow(new OptimisticLockingFailureException("conflict"));

                assertThatThrownBy(() -> testedSubject.suspendMember(guardianId, command, Set.of(groupId)))
                        .isInstanceOf(OptimisticLockingFailureException.class);
            }

            @Test
            @DisplayName("should refuse to suspend a minor the caller has no MEMBERS:MANAGE authority over")
            void shouldRequireAuthorityOverEachMinor() {
                givenFamilyLoadable();
                when(authorizationEvaluator.has(Authority.MEMBERS_MANAGE, TargetRef.member(minorTwoId.uuid())))
                        .thenReturn(false);

                assertThatThrownBy(() -> testedSubject.suspendMember(guardianId, command, Set.of(groupId)))
                        .isInstanceOf(InsufficientAuthorityException.class);

                assertNobodySuspended();
            }

            @Test
            @DisplayName("should behave as a plain suspension without cascade group ids")
            void shouldNotLookUpMinorsWithoutCascade() {
                givenSaveReturnsSavedMember();
                when(memberRepository.findById(guardianId)).thenReturn(Optional.of(testActiveMember));

                testedSubject.suspendMember(guardianId, command, Set.of());

                verifyNoInteractions(legalGuardianCascadePort);
                assertThat(minorOne.isActive()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Sync Member From Oris Tests")
    class SyncMemberFromOrisTests {

        @Test
        @DisplayName("should load, apply and save member on successful sync")
        void shouldLoadApplyAndSaveOnSuccessfulSync() {
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(testMember));
            when(memberCompletenessPort.missingData(testMember)).thenReturn(Set.of(MissingDataItem.BIRTH_NUMBER));
            when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

            var command = MemberSyncFromOrisBuilder.builder()
                    .registrationNumber(testMember.getRegistrationNumber())
                    .firstName("Petr")
                    .lastName("Svoboda")
                    .dateOfBirth(LocalDate.of(1985, 3, 20))
                    .gender(Gender.MALE)
                    .nationality(Nationality.of("SK"))
                    .birthNumber(null)
                    .email(EmailAddress.of("petr.svoboda@example.com"))
                    .phone(PhoneNumber.of("+420999888777"))
                    .address(Address.of("Nová 1", "Brno", "60200", Country.of("CZ")))
                    .chipNumber("111")
                    .build();

            Member result = testedSubject.syncMemberFromOris(new MemberId(testMemberId), command);

            assertThat(result.getFirstName()).isEqualTo("Petr");
            assertThat(result.getLastName()).isEqualTo("Svoboda");
            assertThat(result.getEmail()).isEqualTo(EmailAddress.of("petr.svoboda@example.com"));
            verify(memberCompletenessPort).missingData(testMember);
            assertThat(result.isDataIncomplete()).isTrue();
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("should throw MemberNotFoundException for unknown member id")
        void shouldThrowWhenMemberNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(memberRepository.findById(new MemberId(unknownId))).thenReturn(Optional.empty());

            var command = MemberSyncFromOrisBuilder.builder()
                    .registrationNumber(testMember.getRegistrationNumber())
                    .firstName("Petr")
                    .lastName("Svoboda")
                    .dateOfBirth(LocalDate.of(1985, 3, 20))
                    .gender(Gender.MALE)
                    .nationality(Nationality.of("SK"))
                    .birthNumber(null)
                    .email(EmailAddress.of("petr.svoboda@example.com"))
                    .phone(PhoneNumber.of("+420999888777"))
                    .address(Address.of("Nová 1", "Brno", "60200", Country.of("CZ")))
                    .chipNumber("111")
                    .build();

            assertThatThrownBy(() -> testedSubject.syncMemberFromOris(new MemberId(unknownId), command))
                    .isInstanceOf(MemberNotFoundException.class);

            verify(memberRepository, never()).save(any(Member.class));
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

            Member result = testedSubject.getMemberAndRecordView(new MemberId(testMemberId), new UserId(viewerUserId), new MemberViewAccess(false, false));

            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("should throw MemberNotFoundException for inactive member when caller lacks MANAGE authority")
        void shouldThrow404ForInactiveMemberWithoutManage() {
            Member inactiveMember = MemberTestDataBuilder.aMember().withId(testMemberId).withActive(false).build();
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(inactiveMember));

            assertThatThrownBy(() -> testedSubject.getMemberAndRecordView(new MemberId(testMemberId), new UserId(viewerUserId), new MemberViewAccess(false, false)))
                    .isInstanceOf(MemberNotFoundException.class);

            verify(memberRepository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("should throw MemberNotFoundException when member does not exist")
        void shouldThrowWhenMemberNotFound() {
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testedSubject.getMemberAndRecordView(new MemberId(testMemberId), new UserId(viewerUserId), new MemberViewAccess(false, false)))
                    .isInstanceOf(MemberNotFoundException.class);
        }

        @Test
        @DisplayName("should return inactive member when suspended members are visible to the viewer")
        void shouldReturnInactiveMemberWhenSuspendedMembersAreVisible() {
            Member inactiveMember = MemberTestDataBuilder.aMember().withId(testMemberId).withActive(false).build();
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(inactiveMember));

            Member result = testedSubject.getMemberAndRecordView(new MemberId(testMemberId), new UserId(viewerUserId),
                    new MemberViewAccess(true, true));

            assertThat(result.isActive()).isFalse();
        }

        @Test
        @DisplayName("should publish BirthNumberAccessedEvent for a viewer who sees the birth number")
        void shouldPublishBirthNumberAccessedEventWhenBirthNumberIsVisible() {
            Member memberWithBirthNumber = MemberTestDataBuilder.aMember()
                    .withId(testMemberId)
                    .withNationality("CZ")
                    .withBirthNumber("900101/1234")
                    .build();
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(memberWithBirthNumber));

            testedSubject.getMemberAndRecordView(new MemberId(testMemberId), new UserId(viewerUserId),
                    new MemberViewAccess(true, true));

            ArgumentCaptor<BirthNumberAccessedEvent> event = ArgumentCaptor.forClass(BirthNumberAccessedEvent.class);
            verify(eventPublisher).publishEvent(event.capture());
            assertThat(event.getValue().memberId()).isEqualTo(new MemberId(testMemberId));
        }

        @Test
        @DisplayName("should not publish BirthNumberAccessedEvent when the viewer does not see the birth number")
        void shouldNotPublishEventWhenBirthNumberIsNotVisible() {
            Member memberWithBirthNumber = MemberTestDataBuilder.aMember()
                    .withId(testMemberId)
                    .withNationality("CZ")
                    .withBirthNumber("900101/1234")
                    .build();
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(memberWithBirthNumber));

            testedSubject.getMemberAndRecordView(new MemberId(testMemberId), new UserId(viewerUserId),
                    new MemberViewAccess(false, false));

            verify(eventPublisher, never()).publishEvent(any(BirthNumberAccessedEvent.class));
        }

        @Test
        @DisplayName("should not publish BirthNumberAccessedEvent when member has no birth number")
        void shouldNotPublishEventWhenNoBirthNumber() {
            Member memberWithoutBirthNumber = MemberTestDataBuilder.aMember()
                    .withId(testMemberId)
                    .withNationality("SK")
                    .build();
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(memberWithoutBirthNumber));

            testedSubject.getMemberAndRecordView(new MemberId(testMemberId), new UserId(viewerUserId), new MemberViewAccess(true, true));

            verify(eventPublisher, never()).publishEvent(any(BirthNumberAccessedEvent.class));
        }
    }

    @Nested
    @DisplayName("Get Member Tests")
    class GetMemberTests {

        @Test
        @DisplayName("should return member without publishing BirthNumberAccessedEvent even when member has a birth number")
        void shouldReturnMemberWithoutPublishingAuditEvent() {
            Member memberWithBirthNumber = MemberTestDataBuilder.aMember()
                    .withId(testMemberId)
                    .withNationality("CZ")
                    .withBirthNumber("900101/1234")
                    .build();
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(memberWithBirthNumber));

            Member result = testedSubject.getMember(new MemberId(testMemberId));

            assertThat(result.getId().uuid()).isEqualTo(testMemberId);
            verify(eventPublisher, never()).publishEvent(any(BirthNumberAccessedEvent.class));
        }

        @Test
        @DisplayName("should throw MemberNotFoundException when member does not exist")
        void shouldThrowWhenMemberNotFound() {
            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testedSubject.getMember(new MemberId(testMemberId)))
                    .isInstanceOf(MemberNotFoundException.class);
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
                    .withAddress(Address.of("Reakční 10", "Brno", "60200", Country.of("CZ")))
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
                    .withAddress(Address.of("Řeznická 1", "Brno", "60200", Country.of("CZ")))
                    .build();

            when(memberRepository.findById(new MemberId(testMemberId))).thenReturn(Optional.of(activeMember));

            var command = MemberResumeMembershipBuilder.builder().resumedBy(new UserId(adminUserId)).build();
            assertThatThrownBy(() -> testedSubject.resumeMember(new MemberId(testMemberId), command))
                    .isInstanceOf(MemberAlreadyActiveException.class)
                    .hasMessageContaining("already active");

            verify(memberRepository, never()).save(any(Member.class));
            verify(userService, never()).resumeUser(any(UserId.class));
        }

    }

    @Test
    @DisplayName("listActiveMembers should return only active-filter results")
    void shouldListActiveMembers() {
        when(memberRepository.findAll(MemberFilter.activeOnly())).thenReturn(List.of(testMember));

        assertThat(testedSubject.listActiveMembers()).containsExactly(testMember);
    }
}
