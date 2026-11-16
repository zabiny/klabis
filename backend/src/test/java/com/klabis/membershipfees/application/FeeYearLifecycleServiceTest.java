package com.klabis.membershipfees.application;

import com.klabis.finance.application.ChargePort;
import com.klabis.finance.domain.Money;
import com.klabis.members.MemberId;
import com.klabis.members.application.AllMembersPort;
import com.klabis.membershipfees.FeeSelectionCampaignId;
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
import java.time.Instant;
import java.time.LocalDate;
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

    private FeeYearLifecycleService testedInstance;

    @BeforeEach
    void setUp() {
        testedInstance = new FeeYearLifecycleService(
                campaignRepository, allMembersPort, groupRepository, eventPublisher, chargePort, markerRepository);
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
        @DisplayName("should call processPublication with the loaded campaign and all members")
        void shouldProcessPublication() {
            testedInstance.closeCampaign(CAMPAIGN_ID);

            verify(campaignRepository).save(any(FeeSelectionCampaign.class));
        }

        @Test
        @DisplayName("should mark the campaign as processed after closing")
        void shouldMarkCampaignAsProcessed() {
            testedInstance.closeCampaign(CAMPAIGN_ID);

            verify(campaignRepository).save(argThat(saved -> saved.getDeadlineProcessedAt() != null));
        }

        @Test
        @DisplayName("should fetch all members and pass them to processPublication")
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
        @DisplayName("should throw CampaignAlreadyProcessedException before calling processPublication")
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
            FeeSelectionCampaign campaign = FeeSelectionCampaign.reconstruct(
                    CAMPAIGN_ID,
                    2099,
                    DEADLINE_IN_FUTURE,
                    null,
                    List.of());

            MembershipFeeGroup group = MembershipFeeGroup.reconstruct(
                    new MembershipFeeGroupId(UUID.randomUUID()),
                    new MembershipFeeTierId(UUID.randomUUID()),
                    "Youth",
                    2099, DEADLINE_IN_FUTURE,
                    Money.ofCzk(BigDecimal.valueOf(500)),
                    PublishedLevelStatus.EDITABLE,
                    List.of(),
                    Set.of(new FeeGroupMembership(MEMBER_WITH_CHOICE, LocalDate.of(2099, 1, 15), AssignmentSource.MEMBER_CHOICE, null)),
                    null);

            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_WITH_CHOICE));
            when(groupRepository.findByYear(2099)).thenReturn(List.of(group));
            when(markerRepository.findChargedMemberIdsForYear(2099)).thenReturn(Set.of(MEMBER_WITH_CHOICE));

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
            FeeSelectionCampaign campaign = FeeSelectionCampaign.reconstruct(
                    CAMPAIGN_ID,
                    2099,
                    DEADLINE_IN_FUTURE,
                    null,
                    List.of());

            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_WITHOUT_CHOICE));
            when(groupRepository.findByYear(2099)).thenReturn(List.of());

            testedInstance.closeCampaign(CAMPAIGN_ID);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            assertThat(eventCaptor.getValue())
                    .isInstanceOfSatisfying(com.klabis.membershipfees.MemberMissedFeeSelectionEvent.class, event -> {
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

        private FeeSelectionCampaign publication;
        private MembershipFeeGroup groupWithMemberWithChoice;

        @BeforeEach
        void setUp() {
            publication = FeeSelectionCampaign.reconstruct(
                    new FeeSelectionCampaignId(UUID.randomUUID()),
                    2026,
                    DEADLINE,
                    null,
                    List.of());

            groupWithMemberWithChoice = MembershipFeeGroup.reconstruct(
                    new MembershipFeeGroupId(UUID.randomUUID()),
                    new MembershipFeeTierId(UUID.randomUUID()),
                    "Youth",
                    2026, DEADLINE,
                    Money.ofCzk(BigDecimal.valueOf(500)),
                    PublishedLevelStatus.EDITABLE,
                    List.of(),
                    Set.of(new FeeGroupMembership(MEMBER_WITH_CHOICE, LocalDate.of(2026, 1, 15), AssignmentSource.MEMBER_CHOICE, null)),
                    null);

            when(campaignRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of(publication));
            when(groupRepository.findByYear(2026))
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

            Object publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent).isInstanceOf(com.klabis.membershipfees.MemberMissedFeeSelectionEvent.class);

            com.klabis.membershipfees.MemberMissedFeeSelectionEvent event =
                    (com.klabis.membershipfees.MemberMissedFeeSelectionEvent) publishedEvent;
            assertThat(event.memberId()).isEqualTo(MEMBER_WITHOUT_CHOICE);
            assertThat(event.year()).isEqualTo(2026);
        }

        @Test
        @DisplayName("should not publish event for members who already have a choice")
        void shouldNotPublishEventForMembersWithChoice() {
            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher, atMostOnce()).publishEvent(eventCaptor.capture());

            List<Object> publishedEvents = eventCaptor.getAllValues();
            publishedEvents.forEach(e -> {
                com.klabis.membershipfees.MemberMissedFeeSelectionEvent event =
                        (com.klabis.membershipfees.MemberMissedFeeSelectionEvent) e;
                assertThat(event.memberId()).isNotEqualTo(MEMBER_WITH_CHOICE);
            });
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
        @DisplayName("should freeze all groups for the year before sanctioning members")
        void shouldFreezeGroupsBeforeSanctioning() {
            assertThat(groupWithMemberWithChoice.getStatus()).isEqualTo(PublishedLevelStatus.EDITABLE);

            when(groupRepository.save(any(MembershipFeeGroup.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            ArgumentCaptor<MembershipFeeGroup> groupCaptor = ArgumentCaptor.forClass(MembershipFeeGroup.class);
            verify(groupRepository).save(groupCaptor.capture());
            assertThat(groupCaptor.getValue().getStatus()).isEqualTo(PublishedLevelStatus.FROZEN);
        }

        @Test
        @DisplayName("should both freeze and persist the group and publish sanction events in the same run")
        void shouldFreezeAndPublishInSameRun() {
            List<MembershipFeeGroup> savedGroups = new java.util.ArrayList<>();

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
            when(markerRepository.findChargedMemberIdsForYear(2026)).thenReturn(Set.of());

            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            verify(chargePort, times(1)).chargeMembershipFee(
                    eq(MEMBER_WITH_CHOICE),
                    argThat(amount -> amount.compareTo(BigDecimal.valueOf(500)) == 0),
                    eq(2026));
        }

        @Test
        @DisplayName("should save marker after successful charge")
        void shouldSaveMarkerAfterCharge() {
            when(markerRepository.findChargedMemberIdsForYear(2026)).thenReturn(Set.of());

            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            verify(markerRepository).markCharged(MEMBER_WITH_CHOICE, 2026);
        }

        @Test
        @DisplayName("should skip charge for member who already has a marker (idempotence)")
        void shouldSkipChargeForAlreadyMarkedMember() {
            when(markerRepository.findChargedMemberIdsForYear(2026)).thenReturn(Set.of(MEMBER_WITH_CHOICE));

            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            verifyNoInteractions(chargePort);
            verify(markerRepository, never()).markCharged(any(), anyInt());
        }
    }

    @Nested
    @DisplayName("processCampaignEnd — when publication was already processed")
    class WhenAlreadyProcessed {

        @Test
        @DisplayName("should not process again (idempotence)")
        void shouldNotReprocess() {
            when(campaignRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of());

            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            verifyNoInteractions(allMembersPort, eventPublisher, chargePort, markerRepository);
        }
    }

    @Nested
    @DisplayName("processCampaignEnd — when all members have chosen")
    class WhenAllMembersHaveChosen {

        @Test
        @DisplayName("should not publish any event")
        void shouldPublishNoEvents() {
            FeeSelectionCampaign publication = FeeSelectionCampaign.reconstruct(
                    new FeeSelectionCampaignId(UUID.randomUUID()),
                    2026,
                    DEADLINE,
                    null,
                    List.of());

            MembershipFeeGroup group = MembershipFeeGroup.reconstruct(
                    new MembershipFeeGroupId(UUID.randomUUID()),
                    new MembershipFeeTierId(UUID.randomUUID()),
                    "Youth",
                    2026, DEADLINE,
                    Money.ofCzk(BigDecimal.valueOf(500)),
                    PublishedLevelStatus.EDITABLE,
                    List.of(),
                    Set.of(new FeeGroupMembership(MEMBER_WITH_CHOICE, LocalDate.of(2026, 1, 15), AssignmentSource.MEMBER_CHOICE, null)),
                    null);

            when(campaignRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of(publication));
            when(groupRepository.findByYear(2026)).thenReturn(List.of(group));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_WITH_CHOICE));
            when(markerRepository.findChargedMemberIdsForYear(2026)).thenReturn(Set.of());

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
            FeeSelectionCampaign publication = FeeSelectionCampaign.reconstruct(
                    new FeeSelectionCampaignId(UUID.randomUUID()),
                    2026,
                    DEADLINE,
                    null,
                    List.of());

            MembershipFeeGroup emptyGroup = MembershipFeeGroup.reconstruct(
                    new MembershipFeeGroupId(UUID.randomUUID()),
                    new MembershipFeeTierId(UUID.randomUUID()),
                    "Empty",
                    2026, DEADLINE,
                    Money.ofCzk(BigDecimal.valueOf(500)),
                    PublishedLevelStatus.EDITABLE,
                    List.of(),
                    Set.of(),
                    null);

            when(campaignRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of(publication));
            when(groupRepository.findByYear(2026)).thenReturn(List.of(emptyGroup));
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
            FeeSelectionCampaign publication = FeeSelectionCampaign.reconstruct(
                    new FeeSelectionCampaignId(UUID.randomUUID()),
                    2026,
                    DEADLINE,
                    null,
                    List.of());

            MembershipFeeGroup group = MembershipFeeGroup.reconstruct(
                    new MembershipFeeGroupId(UUID.randomUUID()),
                    new MembershipFeeTierId(UUID.randomUUID()),
                    "Youth",
                    2026, DEADLINE,
                    Money.ofCzk(BigDecimal.valueOf(300)),
                    PublishedLevelStatus.EDITABLE,
                    List.of(),
                    Set.of(new FeeGroupMembership(MEMBER_WITH_CHOICE, LocalDate.of(2026, 1, 15), AssignmentSource.MEMBER_CHOICE, null)),
                    null);

            when(campaignRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of(publication));
            when(groupRepository.findByYear(2026)).thenReturn(List.of(group));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_WITH_CHOICE));
            when(markerRepository.findChargedMemberIdsForYear(2026)).thenReturn(Set.of());

            testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE);

            verify(chargePort).chargeMembershipFee(
                    eq(MEMBER_WITH_CHOICE),
                    argThat(amount -> amount.compareTo(BigDecimal.valueOf(300)) == 0),
                    eq(2026));
        }
    }

    @Nested
    @DisplayName("processCampaignEnd — batch failure")
    class BatchFailure {

        @Test
        @DisplayName("should abort the rest of the batch when processing one campaign fails")
        void shouldAbortBatchWhenOneCampaignFails() {
            FeeSelectionCampaign first = FeeSelectionCampaign.reconstruct(
                    new FeeSelectionCampaignId(UUID.randomUUID()),
                    2026,
                    DEADLINE,
                    null,
                    List.of());
            FeeSelectionCampaign second = FeeSelectionCampaign.reconstruct(
                    new FeeSelectionCampaignId(UUID.randomUUID()),
                    2027,
                    DEADLINE,
                    null,
                    List.of());

            MembershipFeeGroup group = MembershipFeeGroup.reconstruct(
                    new MembershipFeeGroupId(UUID.randomUUID()),
                    new MembershipFeeTierId(UUID.randomUUID()),
                    "Youth",
                    2026, DEADLINE,
                    Money.ofCzk(BigDecimal.valueOf(500)),
                    PublishedLevelStatus.EDITABLE,
                    List.of(),
                    Set.of(new FeeGroupMembership(MEMBER_WITH_CHOICE, LocalDate.of(2026, 1, 15), AssignmentSource.MEMBER_CHOICE, null)),
                    null);

            when(campaignRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of(first, second));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_WITH_CHOICE));
            when(groupRepository.findByYear(2026)).thenReturn(List.of(group));
            when(markerRepository.findChargedMemberIdsForYear(2026)).thenReturn(Set.of());
            doThrow(new IllegalStateException("charge failed"))
                    .when(chargePort).chargeMembershipFee(any(), any(), anyInt());

            assertThatThrownBy(() -> testedInstance.processCampaignEnd(DAY_AFTER_DEADLINE))
                    .isInstanceOf(IllegalStateException.class);

            verify(groupRepository, never()).findByYear(2027);
            verify(campaignRepository, never()).save(any(FeeSelectionCampaign.class));
        }
    }
}
