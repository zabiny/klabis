package com.klabis.membershipfees.application;

import com.klabis.finance.application.ChargePort;
import com.klabis.finance.domain.Money;
import com.klabis.members.MemberId;
import com.klabis.members.application.AllMembersPort;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MemberFeeSelectionResolvedEvent;
import com.klabis.membershipfees.MemberMissedFeeSelectionEvent;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeeYearLifecycleService")
class FeeYearLifecycleServiceTest {

    private static final FeeSelectionCampaignId CAMPAIGN_ID = new FeeSelectionCampaignId(UUID.randomUUID());
    private static final LocalDate DEADLINE_IN_FUTURE = LocalDate.of(2099, 12, 31);
    private static final MemberId MEMBER_ID = new MemberId(UUID.randomUUID());

    private static final LocalDate DEADLINE = LocalDate.of(2026, 3, 31);
    private static final LocalDate DAY_AFTER_DEADLINE = DEADLINE.plusDays(1);

    private static final MemberId MEMBER_WITHOUT_CHOICE = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MemberId MEMBER_WITH_CHOICE = new MemberId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    private static final MemberId ADMIN_ID = new MemberId(UUID.fromString("a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0"));
    private static final MembershipFeeTierId LEVEL_ID_A =
            new MembershipFeeTierId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MembershipFeeTierId LEVEL_ID_B =
            new MembershipFeeTierId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final int FEE_YEAR = 2026;
    private static final BigDecimal FEE_AMOUNT = new BigDecimal("1200.00");
    private static final LocalDate CHOICE_TODAY = LocalDate.of(FEE_YEAR, 3, 15);
    private static final LocalDate CHOICE_GROUP_DEADLINE = CHOICE_TODAY.plusDays(30);

    @Mock
    private FeeSelectionCampaignRepository campaignRepository;
    @Mock
    private AllMembersPort allMembersPort;
    @Mock
    private MembershipFeeGroupRepository groupRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ChargePort chargePort;
    @Mock
    private YearlyFeeChargeMarkerRepository markerRepository;

    private Clock clock;
    private FeeYearLifecycleService testedInstance;

    @BeforeEach
    void setUp() {
        clock = Clock.systemDefaultZone();
        // Most tests verify placement, freezing or event publishing. Defaulting the marker to "already
        // charged" keeps the charge path out of their way; tests that assert a charge stub it explicitly.
        lenient().when(markerRepository.existsByMemberIdAndYear(any(), anyInt())).thenReturn(true);
        testedInstance = newTestedInstance();
    }

    private FeeYearLifecycleService newTestedInstance() {
        return new FeeYearLifecycleService(
                campaignRepository, allMembersPort, groupRepository, eventPublisher, chargePort, markerRepository, clock);
    }

    private MembershipFeeGroup buildGroup(MembershipFeeTierId levelId, String name, LocalDate votingDeadline,
                                          PublishedLevelStatus status, Set<FeeGroupMembership> memberships) {
        return MembershipFeeGroup.reconstruct(
                new MembershipFeeGroupId(UUID.randomUUID()),
                levelId, name, FEE_YEAR, votingDeadline,
                Money.ofCzk(FEE_AMOUNT),
                status, List.of(), memberships, null);
    }

    private MembershipFeeGroup buildFrozenGroup(MembershipFeeTierId levelId) {
        return buildGroup(levelId, "Frozen Group", DEADLINE, PublishedLevelStatus.FROZEN, Set.of());
    }

    private MembershipFeeGroup buildEditableGroup(MembershipFeeTierId levelId) {
        return buildGroup(levelId, "Editable Group", DEADLINE, PublishedLevelStatus.EDITABLE, Set.of());
    }

    private MembershipFeeGroup buildChoiceGroup(MembershipFeeTierId levelId) {
        return buildGroup(levelId, "Test Group", CHOICE_GROUP_DEADLINE, PublishedLevelStatus.EDITABLE, Set.of());
    }

    private MembershipFeeGroup buildYearlyFeeGroup(int year, Money yearlyFee, Set<FeeGroupMembership> memberships) {
        return MembershipFeeGroup.reconstruct(
                new MembershipFeeGroupId(UUID.randomUUID()), LEVEL_ID_A, "Youth", year, DEADLINE,
                yearlyFee, PublishedLevelStatus.EDITABLE, List.of(), memberships, null);
    }

    private static Set<FeeGroupMembership> membershipOf(MemberId memberId, int year) {
        return Set.of(new FeeGroupMembership(memberId, LocalDate.of(year, 1, 15), AssignmentSource.MEMBER_CHOICE, null));
    }

    private FeeSelectionCampaign buildClosedPublication() {
        return buildClosedPublication(FEE_YEAR);
    }

    private FeeSelectionCampaign buildClosedPublication(int year) {
        return FeeSelectionCampaign.reconstruct(
                new FeeSelectionCampaignId(UUID.randomUUID()), year, LocalDate.now().minusDays(1), null, List.of());
    }

    private FeeSelectionCampaign buildProcessedPublication() {
        return FeeSelectionCampaign.reconstruct(
                new FeeSelectionCampaignId(UUID.randomUUID()), FEE_YEAR, LocalDate.now().plusDays(10), Instant.now(), List.of());
    }

    private FeeSelectionCampaign buildOpenCampaign() {
        return FeeSelectionCampaign.reconstruct(
                new FeeSelectionCampaignId(UUID.randomUUID()), FEE_YEAR, LocalDate.now().plusDays(30), null, List.of());
    }

    private FeeSelectionCampaign buildChoiceCampaign() {
        return FeeSelectionCampaign.reconstruct(
                new FeeSelectionCampaignId(UUID.randomUUID()), FEE_YEAR, CHOICE_TODAY.plusDays(30), null, List.of());
    }

    @Nested
    @DisplayName("closeCampaign — when campaign exists and has not been processed yet")
    class CloseCampaignWhenCampaignIsActive {

        private FeeSelectionCampaign activeCampaign;

        @BeforeEach
        void setUp() {
            activeCampaign = FeeSelectionCampaign.reconstruct(
                    CAMPAIGN_ID,
                    2099,
                    DEADLINE_IN_FUTURE,
                    null,
                    List.of());
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(activeCampaign));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_ID));
            when(groupRepository.findByYear(2099)).thenReturn(List.of());
        }

        @Test
        @DisplayName("should mark the campaign as processed after closing")
        void shouldMarkCampaignAsProcessed() {
            testedInstance.closeCampaign(CAMPAIGN_ID);

            verify(campaignRepository).save(argThat(saved -> saved.getDeadlineProcessedAt() != null));
        }

        @Test
        @DisplayName("should fetch all members")
        void shouldFetchAllMembers() {
            testedInstance.closeCampaign(CAMPAIGN_ID);

            verify(allMembersPort).findAll();
        }
    }

    @Nested
    @DisplayName("closeCampaign — when campaign does not exist")
    class CloseCampaignWhenCampaignNotFound {

        @Test
        @DisplayName("should throw FeeSelectionCampaignNotFoundException")
        void shouldThrowWhenCampaignNotFound() {
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testedInstance.closeCampaign(CAMPAIGN_ID))
                    .isInstanceOf(FeeSelectionCampaignNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("closeCampaign — when campaign was already processed")
    class CloseCampaignWhenCampaignAlreadyProcessed {

        @Test
        @DisplayName("should throw CampaignAlreadyProcessedException before processing the campaign")
        void shouldThrowWhenAlreadyProcessed() {
            FeeSelectionCampaign processedCampaign = FeeSelectionCampaign.reconstruct(
                    CAMPAIGN_ID,
                    2020,
                    LocalDate.of(2020, 3, 31),
                    Instant.parse("2020-04-01T00:00:00Z"),
                    List.of());
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(processedCampaign));

            assertThatThrownBy(() -> testedInstance.closeCampaign(CAMPAIGN_ID))
                    .isInstanceOf(CampaignAlreadyProcessedException.class);

            verify(allMembersPort, never()).findAll();
        }
    }

    @Nested
    @DisplayName("closeCampaign — yearly fee charging")
    class CloseCampaignCharging {

        @Test
        @DisplayName("should not charge a member who was already charged by a previous close")
        void shouldNotChargeAlreadyChargedMember() {
            MembershipFeeGroup group = buildYearlyFeeGroup(
                    2099, Money.ofCzk(BigDecimal.valueOf(500)), membershipOf(MEMBER_WITH_CHOICE, 2099));

            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(buildClosedPublication(2099)));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_WITH_CHOICE));
            when(groupRepository.findByYear(2099)).thenReturn(List.of(group));
            when(markerRepository.existsByMemberIdAndYear(MEMBER_WITH_CHOICE, 2099)).thenReturn(true);

            testedInstance.closeCampaign(CAMPAIGN_ID);

            verifyNoInteractions(chargePort);
            verify(markerRepository, never()).markCharged(any(), anyInt());
        }
    }

    @Nested
    @DisplayName("closeCampaign — member without a choice")
    class CloseCampaignSanction {

        @Test
        @DisplayName("should publish MemberMissedFeeSelectionEvent for a member without a choice")
        void shouldPublishSanctionEventForMemberWithoutChoice() {
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(buildClosedPublication(2099)));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_WITHOUT_CHOICE));
            when(groupRepository.findByYear(2099)).thenReturn(List.of());

            testedInstance.closeCampaign(CAMPAIGN_ID);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            assertThat(eventCaptor.getValue())
                    .isInstanceOfSatisfying(MemberMissedFeeSelectionEvent.class, event -> {
                        assertThat(event.memberId()).isEqualTo(MEMBER_WITHOUT_CHOICE);
                        assertThat(event.year()).isEqualTo(2099);
                    });
        }
    }

    @Nested
    @DisplayName("processCampaignEnd — when no unprocessed closed publications exist")
    class WhenNoUnprocessedPublications {

        @Test
        @DisplayName("should do nothing")
        void shouldDoNothing() {
            when(campaignRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of());

            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            verifyNoInteractions(allMembersPort, eventPublisher, chargePort, markerRepository);
        }
    }

    @Nested
    @DisplayName("processCampaignEnd — when a closed unprocessed publication exists")
    class WhenUnprocessedPublicationExists {

        private MembershipFeeGroup groupWithMemberWithChoice;

        @BeforeEach
        void setUp() {
            groupWithMemberWithChoice = buildYearlyFeeGroup(
                    FEE_YEAR, Money.ofCzk(BigDecimal.valueOf(500)), membershipOf(MEMBER_WITH_CHOICE, FEE_YEAR));

            when(campaignRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of(buildClosedPublication()));
            when(groupRepository.findByYear(FEE_YEAR))
                    .thenReturn(List.of(groupWithMemberWithChoice));
            when(allMembersPort.findAll())
                    .thenReturn(Set.of(MEMBER_WITHOUT_CHOICE, MEMBER_WITH_CHOICE));
        }

        @Test
        @DisplayName("should publish MemberMissedFeeSelectionEvent only for members without a choice")
        void shouldPublishEventOnlyForMembersWithoutChoice() {
            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

            assertThat(eventCaptor.getValue())
                    .isInstanceOfSatisfying(MemberMissedFeeSelectionEvent.class, event -> {
                        assertThat(event.memberId()).isEqualTo(MEMBER_WITHOUT_CHOICE);
                        assertThat(event.year()).isEqualTo(FEE_YEAR);
                    });
        }

        @Test
        @DisplayName("should not publish event for members who already have a choice")
        void shouldNotPublishEventForMembersWithChoice() {
            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher, atMostOnce()).publishEvent(eventCaptor.capture());

            assertThat(eventCaptor.getAllValues())
                    .allSatisfy(published -> assertThat(published)
                            .isInstanceOfSatisfying(MemberMissedFeeSelectionEvent.class,
                                    event -> assertThat(event.memberId()).isNotEqualTo(MEMBER_WITH_CHOICE)));
        }

        @Test
        @DisplayName("should mark publication as processed after handling")
        void shouldMarkPublicationAsProcessed() {
            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            ArgumentCaptor<FeeSelectionCampaign> savedCaptor = ArgumentCaptor.forClass(FeeSelectionCampaign.class);
            verify(campaignRepository).save(savedCaptor.capture());

            FeeSelectionCampaign saved = savedCaptor.getValue();
            assertThat(saved.getDeadlineProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("should both freeze and persist the group and publish sanction events in the same run")
        void shouldFreezeAndPublishInSameRun() {
            List<MembershipFeeGroup> savedGroups = new ArrayList<>();

            when(groupRepository.save(any(MembershipFeeGroup.class)))
                    .thenAnswer(inv -> {
                        MembershipFeeGroup g = inv.getArgument(0);
                        savedGroups.add(g);
                        return g;
                    });

            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            assertThat(savedGroups).isNotEmpty();
            assertThat(savedGroups.get(0).getStatus()).isEqualTo(PublishedLevelStatus.FROZEN);
            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getAllValues()).isNotEmpty();
        }

        @Test
        @DisplayName("should charge yearly fee for each member in group")
        void shouldChargeYearlyFeeForMembersInGroup() {
            when(markerRepository.existsByMemberIdAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(false);

            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            verify(chargePort, times(1)).chargeMembershipFee(
                    eq(MEMBER_WITH_CHOICE),
                    argThat(amount -> amount.compareTo(BigDecimal.valueOf(500)) == 0),
                    eq(FEE_YEAR));
        }

        @Test
        @DisplayName("should save marker after successful charge")
        void shouldSaveMarkerAfterCharge() {
            when(markerRepository.existsByMemberIdAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(false);

            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            verify(markerRepository).markCharged(MEMBER_WITH_CHOICE, FEE_YEAR);
        }

        @Test
        @DisplayName("should skip charge for member who already has a marker (idempotence)")
        void shouldSkipChargeForAlreadyMarkedMember() {
            when(markerRepository.existsByMemberIdAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(true);

            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            verifyNoInteractions(chargePort);
            verify(markerRepository, never()).markCharged(any(), anyInt());
        }
    }

    @Nested
    @DisplayName("processCampaignEnd — when all members have chosen")
    class WhenAllMembersHaveChosen {

        @Test
        @DisplayName("should not publish any event")
        void shouldPublishNoEvents() {
            MembershipFeeGroup group = buildYearlyFeeGroup(
                    FEE_YEAR, Money.ofCzk(BigDecimal.valueOf(500)), membershipOf(MEMBER_WITH_CHOICE, FEE_YEAR));

            when(campaignRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of(buildClosedPublication()));
            when(groupRepository.findByYear(FEE_YEAR)).thenReturn(List.of(group));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_WITH_CHOICE));
            when(markerRepository.existsByMemberIdAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(false);

            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            verifyNoInteractions(eventPublisher);
            verify(chargePort, times(1)).chargeMembershipFee(any(), any(), anyInt());
            verify(campaignRepository).save(any(FeeSelectionCampaign.class));
        }
    }

    @Nested
    @DisplayName("processCampaignEnd — yearly fee charging, member not in any group")
    class MemberNotInAnyGroup {

        @Test
        @DisplayName("should not charge members not belonging to any group")
        void shouldNotChargeMembersWithoutGroup() {
            MembershipFeeGroup emptyGroup = buildYearlyFeeGroup(
                    FEE_YEAR, Money.ofCzk(BigDecimal.valueOf(500)), Set.of());

            when(campaignRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of(buildClosedPublication()));
            when(groupRepository.findByYear(FEE_YEAR)).thenReturn(List.of(emptyGroup));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_WITHOUT_CHOICE));

            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            verifyNoInteractions(chargePort);
        }
    }

    @Nested
    @DisplayName("processCampaignEnd — yearly fee charging, no overdraft limit enforcement")
    class YearlyFeeChargeUnlimited {

        @Test
        @DisplayName("should charge yearly fee even when it would push balance below overdraft limit")
        void shouldChargeYearlyFeeRegardlessOfOverdraftLimit() {
            // Verifies routing: yearly fee is dispatched via the unlimited chargeMembershipFee() path.
            // Domain-level bypass of overdraft enforcement is covered in MemberAccountTest.
            MembershipFeeGroup group = buildYearlyFeeGroup(
                    FEE_YEAR, Money.ofCzk(BigDecimal.valueOf(300)), membershipOf(MEMBER_WITH_CHOICE, FEE_YEAR));

            when(campaignRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of(buildClosedPublication()));
            when(groupRepository.findByYear(FEE_YEAR)).thenReturn(List.of(group));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_WITH_CHOICE));
            when(markerRepository.existsByMemberIdAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(false);

            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            verify(chargePort).chargeMembershipFee(
                    eq(MEMBER_WITH_CHOICE),
                    argThat(amount -> amount.compareTo(BigDecimal.valueOf(300)) == 0),
                    eq(FEE_YEAR));
        }
    }

    @Nested
    @DisplayName("processCampaignEnd — batch failure")
    class BatchFailure {

        @Test
        @DisplayName("should abort the rest of the batch when processing one campaign fails")
        void shouldAbortBatchWhenOneCampaignFails() {
            MembershipFeeGroup group = buildYearlyFeeGroup(
                    FEE_YEAR, Money.ofCzk(BigDecimal.valueOf(500)), membershipOf(MEMBER_WITH_CHOICE, FEE_YEAR));

            when(campaignRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of(buildClosedPublication(2026), buildClosedPublication(2027)));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_WITH_CHOICE));
            when(groupRepository.findByYear(2026)).thenReturn(List.of(group));
            when(markerRepository.existsByMemberIdAndYear(MEMBER_WITH_CHOICE, 2026)).thenReturn(false);
            doThrow(new IllegalStateException("charge failed"))
                    .when(chargePort).chargeMembershipFee(any(), any(), anyInt());

            assertThatThrownBy(() -> testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE))
                    .isInstanceOf(IllegalStateException.class);

            verify(groupRepository, never()).findByYear(2027);
            verify(campaignRepository, never()).save(any(FeeSelectionCampaign.class));
        }
    }

    @Nested
    @DisplayName("assignLevel() — admin assigns after deadline")
    class AssignLevel {

        @Test
        @DisplayName("should assign member to FROZEN group bypassing deadline check")
        void shouldAssignMemberToFrozenGroupWithoutDeadlineCheck() {
            MembershipFeeGroup frozenGroup = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = frozenGroup.getId();

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(frozenGroup));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildClosedPublication()));
            when(groupRepository.findByMemberAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            testedInstance.assignLevel(new FeeYearLifecyclePort.AssignFeeLevel(
                    ADMIN_ID, MEMBER_WITH_CHOICE, groupId, FEE_YEAR));

            verify(groupRepository).save(argThat(g -> g.hasMember(MEMBER_WITH_CHOICE)));
        }

        @Test
        @DisplayName("should record ADMIN_ASSIGNMENT as source on the membership")
        void shouldRecordAdminAssignmentSource() {
            MembershipFeeGroup frozenGroup = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = frozenGroup.getId();

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(frozenGroup));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildClosedPublication()));
            when(groupRepository.findByMemberAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            testedInstance.assignLevel(new FeeYearLifecyclePort.AssignFeeLevel(
                    ADMIN_ID, MEMBER_WITH_CHOICE, groupId, FEE_YEAR));

            ArgumentCaptor<MembershipFeeGroup> captor = ArgumentCaptor.forClass(MembershipFeeGroup.class);
            verify(groupRepository).save(captor.capture());

            FeeGroupMembership membership = captor.getValue().getMemberships().iterator().next();
            assertThat(membership.source()).isEqualTo(AssignmentSource.ADMIN_ASSIGNMENT);
        }

        @Test
        @DisplayName("should record the adminId as assignedBy on the membership")
        void shouldRecordAdminIdAsAssignedBy() {
            MembershipFeeGroup frozenGroup = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = frozenGroup.getId();

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(frozenGroup));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildClosedPublication()));
            when(groupRepository.findByMemberAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            testedInstance.assignLevel(new FeeYearLifecyclePort.AssignFeeLevel(
                    ADMIN_ID, MEMBER_WITH_CHOICE, groupId, FEE_YEAR));

            ArgumentCaptor<MembershipFeeGroup> captor = ArgumentCaptor.forClass(MembershipFeeGroup.class);
            verify(groupRepository).save(captor.capture());

            FeeGroupMembership membership = captor.getValue().getMemberships().iterator().next();
            assertThat(membership.assignedBy()).isEqualTo(ADMIN_ID);
        }

        @Test
        @DisplayName("should move member from old group to new group when changing assignment after deadline")
        void shouldMoveMemberBetweenGroupsAfterDeadline() {
            Set<FeeGroupMembership> existingMemberships = membershipOf(MEMBER_WITH_CHOICE, FEE_YEAR);
            MembershipFeeGroup oldGroup = buildGroup(LEVEL_ID_A, "Old Frozen Group", DEADLINE,
                    PublishedLevelStatus.FROZEN, existingMemberships);
            MembershipFeeGroup newGroup = buildFrozenGroup(LEVEL_ID_B);

            MembershipFeeGroupId newGroupId = newGroup.getId();
            when(groupRepository.findById(newGroupId)).thenReturn(Optional.of(newGroup));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildClosedPublication()));
            when(groupRepository.findByMemberAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(Optional.of(oldGroup));
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            testedInstance.assignLevel(new FeeYearLifecyclePort.AssignFeeLevel(
                    ADMIN_ID, MEMBER_WITH_CHOICE, newGroupId, FEE_YEAR));

            verify(groupRepository).save(argThat(g -> !g.hasMember(MEMBER_WITH_CHOICE) && g.getId().equals(oldGroup.getId())));
            verify(groupRepository).save(argThat(g -> g.hasMember(MEMBER_WITH_CHOICE) && g.getId().equals(newGroupId)));
        }

        @Test
        @DisplayName("should assign member to EDITABLE group as well (admin works regardless of status)")
        void shouldAssignMemberToEditableGroupToo() {
            MembershipFeeGroup editableGroup = buildEditableGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = editableGroup.getId();

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(editableGroup));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildClosedPublication()));
            when(groupRepository.findByMemberAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            testedInstance.assignLevel(new FeeYearLifecyclePort.AssignFeeLevel(
                    ADMIN_ID, MEMBER_WITH_CHOICE, groupId, FEE_YEAR));

            verify(groupRepository).save(argThat(g -> g.hasMember(MEMBER_WITH_CHOICE)));
        }

        @Test
        @DisplayName("should throw MembershipFeeGroupNotFoundException when group does not exist")
        void shouldThrowWhenGroupNotFound() {
            MembershipFeeGroupId unknownGroupId = new MembershipFeeGroupId(UUID.randomUUID());
            when(groupRepository.findById(unknownGroupId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testedInstance.assignLevel(new FeeYearLifecyclePort.AssignFeeLevel(
                    ADMIN_ID, MEMBER_WITH_CHOICE, unknownGroupId, FEE_YEAR)))
                    .isInstanceOf(MembershipFeeGroupNotFoundException.class);
        }

        @Test
        @DisplayName("should throw FeeSelectionCampaignNotFoundException when no publication for year")
        void shouldThrowWhenNoPublication() {
            MembershipFeeGroup group = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = group.getId();
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testedInstance.assignLevel(new FeeYearLifecyclePort.AssignFeeLevel(
                    ADMIN_ID, MEMBER_WITH_CHOICE, groupId, FEE_YEAR)))
                    .isInstanceOf(FeeSelectionCampaignNotFoundException.class);
        }

        @Test
        @DisplayName("should reject a command whose year does not match the target group's year")
        void shouldRejectCommandYearMismatch() {
            MembershipFeeGroup group = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = group.getId();
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

            assertThatThrownBy(() -> testedInstance.assignLevel(new FeeYearLifecyclePort.AssignFeeLevel(
                    ADMIN_ID, MEMBER_WITH_CHOICE, groupId, FEE_YEAR + 1)))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(campaignRepository, never()).findByYear(anyInt());
            verify(groupRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("assignLevel() — sanction lifting")
    class AssignLevelSanctionLifting {

        @Test
        @DisplayName("should always publish MemberFeeSelectionResolvedEvent on admin assignment")
        void shouldPublishResolvedEventOnAdminAssignment() {
            MembershipFeeGroup group = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = group.getId();

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildClosedPublication()));
            when(groupRepository.findByMemberAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            testedInstance.assignLevel(new FeeYearLifecyclePort.AssignFeeLevel(
                    ADMIN_ID, MEMBER_WITH_CHOICE, groupId, FEE_YEAR));

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue())
                    .isInstanceOfSatisfying(MemberFeeSelectionResolvedEvent.class, event -> {
                        assertThat(event.memberId()).isEqualTo(MEMBER_WITH_CHOICE);
                        assertThat(event.year()).isEqualTo(FEE_YEAR);
                    });
        }
    }

    @Nested
    @DisplayName("assignLevel() — immediate fee charge after campaign closure")
    class ImmediateFeeCharge {

        @Test
        @DisplayName("should NOT charge when campaign is still open (before deadline)")
        void shouldNotChargeWhenCampaignIsOpen() {
            MembershipFeeGroup group = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = group.getId();

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildOpenCampaign()));
            when(groupRepository.findByMemberAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            testedInstance.assignLevel(new FeeYearLifecyclePort.AssignFeeLevel(
                    ADMIN_ID, MEMBER_WITH_CHOICE, groupId, FEE_YEAR));

            verify(chargePort, never()).chargeMembershipFee(any(), any(), anyInt());
            verify(markerRepository, never()).markCharged(any(), anyInt());
        }

        @Test
        @DisplayName("should charge immediately when campaign deadline has passed")
        void shouldChargeImmediatelyWhenCampaignDeadlinePassed() {
            MembershipFeeGroup group = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = group.getId();

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildClosedPublication()));
            when(groupRepository.findByMemberAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(markerRepository.existsByMemberIdAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(false);

            testedInstance.assignLevel(new FeeYearLifecyclePort.AssignFeeLevel(
                    ADMIN_ID, MEMBER_WITH_CHOICE, groupId, FEE_YEAR));

            verify(chargePort).chargeMembershipFee(
                    eq(MEMBER_WITH_CHOICE),
                    argThat(amount -> amount.compareTo(FEE_AMOUNT) == 0),
                    eq(FEE_YEAR));
            verify(markerRepository).markCharged(MEMBER_WITH_CHOICE, FEE_YEAR);
        }

        @Test
        @DisplayName("should charge immediately when campaign is already processed")
        void shouldChargeImmediatelyWhenCampaignIsProcessed() {
            MembershipFeeGroup group = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = group.getId();

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildProcessedPublication()));
            when(groupRepository.findByMemberAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(markerRepository.existsByMemberIdAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(false);

            testedInstance.assignLevel(new FeeYearLifecyclePort.AssignFeeLevel(
                    ADMIN_ID, MEMBER_WITH_CHOICE, groupId, FEE_YEAR));

            verify(chargePort).chargeMembershipFee(any(), any(), anyInt());
            verify(markerRepository).markCharged(MEMBER_WITH_CHOICE, FEE_YEAR);
        }

        @Test
        @DisplayName("should NOT charge when fee marker already exists (deduplication)")
        void shouldNotChargeWhenMarkerAlreadyExists() {
            MembershipFeeGroup group = buildFrozenGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = group.getId();

            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildClosedPublication()));
            when(groupRepository.findByMemberAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(markerRepository.existsByMemberIdAndYear(MEMBER_WITH_CHOICE, FEE_YEAR)).thenReturn(true);

            testedInstance.assignLevel(new FeeYearLifecyclePort.AssignFeeLevel(
                    ADMIN_ID, MEMBER_WITH_CHOICE, groupId, FEE_YEAR));

            verify(chargePort, never()).chargeMembershipFee(any(), any(), anyInt());
            verify(markerRepository, never()).markCharged(any(), anyInt());
        }
    }

    @Nested
    @DisplayName("chooseFeeLevel()")
    class ChooseFeeLevel {

        @BeforeEach
        void useFixedClock() {
            clock = Clock.fixed(CHOICE_TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
            testedInstance = newTestedInstance();
        }

        @Test
        @DisplayName("should add member to the chosen group when voting is open")
        void shouldAddMemberToGroup() {
            MembershipFeeGroup group = buildChoiceGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = group.getId();
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildChoiceCampaign()));
            when(groupRepository.findByMemberAndYear(MEMBER_WITHOUT_CHOICE, FEE_YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            testedInstance.chooseFeeLevel(new FeeYearLifecyclePort.ChooseFeeLevel(MEMBER_WITHOUT_CHOICE, groupId, FEE_YEAR));

            verify(groupRepository).save(argThat(g -> g.hasMember(MEMBER_WITHOUT_CHOICE)));
        }

        @Test
        @DisplayName("should move member from old group to new group when changing choice")
        void shouldMoveMemberBetweenGroups() {
            MembershipFeeGroup oldGroup = buildChoiceGroup(LEVEL_ID_A);
            oldGroup.addMember(MEMBER_WITHOUT_CHOICE, LocalDate.of(FEE_YEAR, 1, 5), AssignmentSource.MEMBER_CHOICE);
            MembershipFeeGroup newGroup = buildChoiceGroup(LEVEL_ID_B);

            MembershipFeeGroupId newGroupId = newGroup.getId();
            when(groupRepository.findById(newGroupId)).thenReturn(Optional.of(newGroup));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildChoiceCampaign()));
            when(groupRepository.findByMemberAndYear(MEMBER_WITHOUT_CHOICE, FEE_YEAR)).thenReturn(Optional.of(oldGroup));
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            testedInstance.chooseFeeLevel(new FeeYearLifecyclePort.ChooseFeeLevel(MEMBER_WITHOUT_CHOICE, newGroupId, FEE_YEAR));

            verify(groupRepository).save(argThat(g -> !g.hasMember(MEMBER_WITHOUT_CHOICE) && g.getId().equals(oldGroup.getId())));
            verify(groupRepository).save(argThat(g -> g.hasMember(MEMBER_WITHOUT_CHOICE) && g.getId().equals(newGroupId)));
        }

        @Test
        @DisplayName("should throw VotingClosedException when group voting deadline has passed")
        void shouldThrowWhenVotingClosed() {
            MembershipFeeGroup group = buildGroup(LEVEL_ID_A, "Expired Group", CHOICE_TODAY.minusDays(1),
                    PublishedLevelStatus.EDITABLE, Set.of());
            MembershipFeeGroupId groupId = group.getId();
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildChoiceCampaign()));

            assertThatThrownBy(() -> testedInstance.chooseFeeLevel(
                    new FeeYearLifecyclePort.ChooseFeeLevel(MEMBER_WITHOUT_CHOICE, groupId, FEE_YEAR)))
                    .isInstanceOf(VotingClosedException.class);
        }

        @Test
        @DisplayName("should throw FeeSelectionCampaignNotFoundException when no publication for year")
        void shouldThrowWhenNoPublication() {
            MembershipFeeGroup group = buildChoiceGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = group.getId();
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testedInstance.chooseFeeLevel(
                    new FeeYearLifecyclePort.ChooseFeeLevel(MEMBER_WITHOUT_CHOICE, groupId, FEE_YEAR)))
                    .isInstanceOf(FeeSelectionCampaignNotFoundException.class);
        }

        @Test
        @DisplayName("should allow choice on the group deadline day itself (boundary: deadline not yet passed)")
        void shouldAllowChoiceOnDeadlineDay() {
            MembershipFeeGroup group = buildGroup(LEVEL_ID_A, "Deadline Today", CHOICE_TODAY,
                    PublishedLevelStatus.EDITABLE, Set.of());
            MembershipFeeGroupId groupId = group.getId();
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildChoiceCampaign()));
            when(groupRepository.findByMemberAndYear(MEMBER_WITHOUT_CHOICE, FEE_YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            testedInstance.chooseFeeLevel(new FeeYearLifecyclePort.ChooseFeeLevel(MEMBER_WITHOUT_CHOICE, groupId, FEE_YEAR));

            verify(groupRepository).save(argThat(g -> g.hasMember(MEMBER_WITHOUT_CHOICE)));
        }

        @Test
        @DisplayName("should throw VotingClosedException the day after the group deadline")
        void shouldThrowDayAfterDeadline() {
            MembershipFeeGroup group = buildGroup(LEVEL_ID_A, "Deadline Yesterday", CHOICE_TODAY.minusDays(1),
                    PublishedLevelStatus.EDITABLE, Set.of());
            MembershipFeeGroupId groupId = group.getId();
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildChoiceCampaign()));

            assertThatThrownBy(() -> testedInstance.chooseFeeLevel(
                    new FeeYearLifecyclePort.ChooseFeeLevel(MEMBER_WITHOUT_CHOICE, groupId, FEE_YEAR)))
                    .isInstanceOf(VotingClosedException.class);
        }

        @Test
        @DisplayName("should reject a command whose year does not match the target group's year")
        void shouldRejectCommandYearMismatch() {
            MembershipFeeGroup group = buildChoiceGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = group.getId();
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

            assertThatThrownBy(() -> testedInstance.chooseFeeLevel(
                    new FeeYearLifecyclePort.ChooseFeeLevel(MEMBER_WITHOUT_CHOICE, groupId, FEE_YEAR - 1)))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(campaignRepository, never()).findByYear(anyInt());
            verify(groupRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeFeeChoice()")
    class RemoveFeeChoice {

        @BeforeEach
        void useFixedClock() {
            clock = Clock.fixed(CHOICE_TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
            testedInstance = newTestedInstance();
        }

        @Test
        @DisplayName("should remove member from their current group when voting is open")
        void shouldRemoveMemberFromGroup() {
            MembershipFeeGroup group = buildChoiceGroup(LEVEL_ID_A);
            group.addMember(MEMBER_WITHOUT_CHOICE, LocalDate.of(FEE_YEAR, 1, 5), AssignmentSource.MEMBER_CHOICE);
            when(groupRepository.findByMemberAndYear(MEMBER_WITHOUT_CHOICE, FEE_YEAR)).thenReturn(Optional.of(group));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildChoiceCampaign()));
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            testedInstance.removeFeeChoice(MEMBER_WITHOUT_CHOICE, FEE_YEAR);

            verify(groupRepository).save(argThat(g -> !g.hasMember(MEMBER_WITHOUT_CHOICE)));
        }

        @Test
        @DisplayName("should be no-op when member has no choice")
        void shouldBeNoOpWhenNoChoice() {
            when(groupRepository.findByMemberAndYear(MEMBER_WITHOUT_CHOICE, FEE_YEAR)).thenReturn(Optional.empty());
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildChoiceCampaign()));

            testedInstance.removeFeeChoice(MEMBER_WITHOUT_CHOICE, FEE_YEAR);

            verify(groupRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw VotingClosedException when removing after group voting deadline")
        void shouldThrowWhenRemovingAfterDeadline() {
            MembershipFeeGroup group = buildGroup(LEVEL_ID_A, "Expired Group", CHOICE_TODAY.minusDays(1),
                    PublishedLevelStatus.EDITABLE, membershipOf(MEMBER_WITHOUT_CHOICE, FEE_YEAR));
            when(campaignRepository.findByYear(FEE_YEAR)).thenReturn(Optional.of(buildChoiceCampaign()));
            when(groupRepository.findByMemberAndYear(MEMBER_WITHOUT_CHOICE, FEE_YEAR)).thenReturn(Optional.of(group));

            assertThatThrownBy(() -> testedInstance.removeFeeChoice(MEMBER_WITHOUT_CHOICE, FEE_YEAR))
                    .isInstanceOf(VotingClosedException.class);
        }
    }
}
