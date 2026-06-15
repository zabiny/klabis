package com.klabis.membershipfees.infrastructure.scheduler;

import com.klabis.finance.application.ChargePort;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeeSelectionDeadlineScheduler")
class FeeSelectionDeadlineSchedulerTest {

    private static final LocalDate DEADLINE = LocalDate.of(2026, 3, 31);
    private static final LocalDate DAY_AFTER_DEADLINE = DEADLINE.plusDays(1);

    private static final MemberId MEMBER_WITHOUT_CHOICE = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MemberId MEMBER_WITH_CHOICE = new MemberId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    @Mock
    private FeeSelectionCampaignRepository publicationRepository;
    @Mock
    private MembershipFeeGroupRepository groupRepository;
    @Mock
    private AllMembersPort allMembersPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ChargePort chargePort;
    @Mock
    private YearlyFeeChargeMarkerRepository markerRepository;

    private FeeSelectionDeadlineScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new FeeSelectionDeadlineScheduler(
                publicationRepository, groupRepository, allMembersPort, eventPublisher,
                chargePort, markerRepository);
    }

    @Nested
    @DisplayName("when no unprocessed closed publications exist")
    class WhenNoUnprocessedPublications {

        @Test
        @DisplayName("should do nothing")
        void shouldDoNothing() {
            when(publicationRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of());

            scheduler.processMissedSelections(DAY_AFTER_DEADLINE);

            verifyNoInteractions(allMembersPort, eventPublisher, chargePort, markerRepository);
        }
    }

    @Nested
    @DisplayName("when a closed unprocessed publication exists")
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
                    com.klabis.finance.domain.Money.ofCzk(BigDecimal.valueOf(500)),
                    PublishedLevelStatus.EDITABLE,
                    List.of(),
                    Set.of(new FeeGroupMembership(MEMBER_WITH_CHOICE, LocalDate.of(2026, 1, 15), AssignmentSource.MEMBER_CHOICE, null)),
                    null);

            when(publicationRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of(publication));
            when(groupRepository.findByYear(2026))
                    .thenReturn(List.of(groupWithMemberWithChoice));
            when(allMembersPort.findAll())
                    .thenReturn(Set.of(MEMBER_WITHOUT_CHOICE, MEMBER_WITH_CHOICE));
        }

        @Test
        @DisplayName("should publish MemberMissedFeeSelectionEvent only for members without a choice")
        void shouldPublishEventOnlyForMembersWithoutChoice() {
            scheduler.processMissedSelections(DAY_AFTER_DEADLINE);

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
            scheduler.processMissedSelections(DAY_AFTER_DEADLINE);

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
            scheduler.processMissedSelections(DAY_AFTER_DEADLINE);

            ArgumentCaptor<FeeSelectionCampaign> savedCaptor = ArgumentCaptor.forClass(FeeSelectionCampaign.class);
            verify(publicationRepository).save(savedCaptor.capture());

            FeeSelectionCampaign saved = savedCaptor.getValue();
            assertThat(saved.getDeadlineProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("should freeze all groups for the year before sanctioning members")
        void shouldFreezeGroupsBeforeSanctioning() {
            assertThat(groupWithMemberWithChoice.getStatus()).isEqualTo(PublishedLevelStatus.EDITABLE);

            when(groupRepository.save(any(MembershipFeeGroup.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            scheduler.processMissedSelections(DAY_AFTER_DEADLINE);

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

            scheduler.processMissedSelections(DAY_AFTER_DEADLINE);

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

            scheduler.processMissedSelections(DAY_AFTER_DEADLINE);

            ArgumentCaptor<ChargePort.ChargeCommand> chargeCaptor = ArgumentCaptor.forClass(ChargePort.ChargeCommand.class);
            verify(chargePort, times(1)).charge(chargeCaptor.capture());

            ChargePort.ChargeCommand command = chargeCaptor.getValue();
            assertThat(command.memberId()).isEqualTo(MEMBER_WITH_CHOICE);
            assertThat(command.amount()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertThat(command.occurredAt()).isEqualTo(DAY_AFTER_DEADLINE);
            assertThat(command.note()).contains("2026");
        }

        @Test
        @DisplayName("should save marker after successful charge")
        void shouldSaveMarkerAfterCharge() {
            when(markerRepository.findChargedMemberIdsForYear(2026)).thenReturn(Set.of());

            scheduler.processMissedSelections(DAY_AFTER_DEADLINE);

            verify(markerRepository).markCharged(MEMBER_WITH_CHOICE, 2026);
        }

        @Test
        @DisplayName("should skip charge for member who already has a marker (idempotence)")
        void shouldSkipChargeForAlreadyMarkedMember() {
            when(markerRepository.findChargedMemberIdsForYear(2026)).thenReturn(Set.of(MEMBER_WITH_CHOICE));

            scheduler.processMissedSelections(DAY_AFTER_DEADLINE);

            verifyNoInteractions(chargePort);
            verify(markerRepository, never()).markCharged(any(), anyInt());
        }
    }

    @Nested
    @DisplayName("when publication was already processed")
    class WhenAlreadyProcessed {

        @Test
        @DisplayName("should not process again (idempotence)")
        void shouldNotReprocess() {
            when(publicationRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of());

            scheduler.processMissedSelections(DAY_AFTER_DEADLINE);

            verifyNoInteractions(allMembersPort, eventPublisher, chargePort, markerRepository);
        }
    }

    @Nested
    @DisplayName("when all members have chosen")
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
                    com.klabis.finance.domain.Money.ofCzk(BigDecimal.valueOf(500)),
                    PublishedLevelStatus.EDITABLE,
                    List.of(),
                    Set.of(new FeeGroupMembership(MEMBER_WITH_CHOICE, LocalDate.of(2026, 1, 15), AssignmentSource.MEMBER_CHOICE, null)),
                    null);

            when(publicationRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of(publication));
            when(groupRepository.findByYear(2026)).thenReturn(List.of(group));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_WITH_CHOICE));
            when(markerRepository.findChargedMemberIdsForYear(2026)).thenReturn(Set.of());

            scheduler.processMissedSelections(DAY_AFTER_DEADLINE);

            verifyNoInteractions(eventPublisher);
            verify(chargePort, times(1)).charge(any(ChargePort.ChargeCommand.class));
            verify(publicationRepository).save(any(FeeSelectionCampaign.class));
        }
    }

    @Nested
    @DisplayName("yearly fee charging — member not in any group")
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
                    com.klabis.finance.domain.Money.ofCzk(BigDecimal.valueOf(500)),
                    PublishedLevelStatus.EDITABLE,
                    List.of(),
                    Set.of(),
                    null);

            when(publicationRepository.findUnprocessedClosedPublications(DAY_AFTER_DEADLINE))
                    .thenReturn(List.of(publication));
            when(groupRepository.findByYear(2026)).thenReturn(List.of(emptyGroup));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_WITHOUT_CHOICE));

            scheduler.processMissedSelections(DAY_AFTER_DEADLINE);

            verifyNoInteractions(chargePort);
        }
    }
}
