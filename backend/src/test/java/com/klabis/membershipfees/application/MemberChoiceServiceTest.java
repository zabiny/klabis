package com.klabis.membershipfees.application;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.membershipfees.domain.FeeGroupMembership;
import com.klabis.membershipfees.domain.FeeYearPublication;
import com.klabis.membershipfees.domain.FeeYearPublicationRepository;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import com.klabis.membershipfees.domain.PublishedLevelStatus;
import com.klabis.membershipfees.domain.VotingClosedException;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberChoiceService")
class MemberChoiceServiceTest {

    private static final MemberId MEMBER_ID = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MembershipFeeLevelId LEVEL_ID_A =
            new MembershipFeeLevelId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MembershipFeeLevelId LEVEL_ID_B =
            new MembershipFeeLevelId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final int YEAR = 2026;

    @Mock
    private MembershipFeeGroupRepository groupRepository;
    @Mock
    private FeeYearPublicationRepository publicationRepository;

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 15);
    private final Clock fixedClock = Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    private MemberChoiceService service;

    @BeforeEach
    void setUp() {
        service = new MemberChoiceService(groupRepository, publicationRepository, fixedClock);
    }

    private static final LocalDate GROUP_DEADLINE = TODAY.plusDays(30);

    private MembershipFeeGroup buildEditableGroup(MembershipFeeLevelId sourceLevelId) {
        return MembershipFeeGroup.reconstruct(
                new MembershipFeeGroupId(UUID.randomUUID()),
                sourceLevelId,
                "Test Group", YEAR, GROUP_DEADLINE,
                Money.ofCzk(new BigDecimal("1200.00")),
                PublishedLevelStatus.EDITABLE,
                List.of(), Set.of(), null);
    }

    private FeeYearPublication buildOpenPublication() {
        return FeeYearPublication.reconstruct(
                new com.klabis.membershipfees.FeeYearPublicationId(UUID.randomUUID()),
                YEAR,
                TODAY.plusDays(30),
                null,
                List.of());
    }

    @Nested
    @DisplayName("chooseFeeLevel()")
    class ChooseFeeLevel {

        @Test
        @DisplayName("should add member to the chosen group when voting is open")
        void shouldAddMemberToGroup() {
            MembershipFeeGroup group = buildEditableGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = group.getId();
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.of(buildOpenPublication()));
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.chooseFeeLevel(new MemberChoicePort.ChooseFeeLevel(MEMBER_ID, groupId, YEAR));

            verify(groupRepository).save(argThat(g -> g.hasMember(MEMBER_ID)));
        }

        @Test
        @DisplayName("should move member from old group to new group when changing choice")
        void shouldMoveMemberBetweenGroups() {
            MembershipFeeGroup oldGroup = buildEditableGroup(LEVEL_ID_A);
            oldGroup.addMember(MEMBER_ID, LocalDate.of(YEAR, 1, 5), AssignmentSource.MEMBER_CHOICE);
            MembershipFeeGroup newGroup = buildEditableGroup(LEVEL_ID_B);

            MembershipFeeGroupId newGroupId = newGroup.getId();
            when(groupRepository.findById(newGroupId)).thenReturn(Optional.of(newGroup));
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.of(buildOpenPublication()));
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.of(oldGroup));
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.chooseFeeLevel(new MemberChoicePort.ChooseFeeLevel(MEMBER_ID, newGroupId, YEAR));

            verify(groupRepository).save(argThat(g -> !g.hasMember(MEMBER_ID) && g.getId().equals(oldGroup.getId())));
            verify(groupRepository).save(argThat(g -> g.hasMember(MEMBER_ID) && g.getId().equals(newGroupId)));
        }

        @Test
        @DisplayName("should throw VotingClosedException when group voting deadline has passed")
        void shouldThrowWhenVotingClosed() {
            MembershipFeeGroup group = MembershipFeeGroup.reconstruct(
                    new MembershipFeeGroupId(UUID.randomUUID()),
                    LEVEL_ID_A,
                    "Expired Group", YEAR, TODAY.minusDays(1),
                    Money.ofCzk(new BigDecimal("1200.00")),
                    PublishedLevelStatus.EDITABLE,
                    List.of(), Set.of(), null);
            MembershipFeeGroupId groupId = group.getId();
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.of(buildOpenPublication()));

            assertThatThrownBy(() -> service.chooseFeeLevel(
                    new MemberChoicePort.ChooseFeeLevel(MEMBER_ID, groupId, YEAR)))
                    .isInstanceOf(VotingClosedException.class);
        }

        @Test
        @DisplayName("should throw FeeYearPublicationNotFoundException when no publication for year")
        void shouldThrowWhenNoPublication() {
            MembershipFeeGroup group = buildEditableGroup(LEVEL_ID_A);
            MembershipFeeGroupId groupId = group.getId();
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.chooseFeeLevel(
                    new MemberChoicePort.ChooseFeeLevel(MEMBER_ID, groupId, YEAR)))
                    .isInstanceOf(FeeYearPublicationNotFoundException.class);
        }

        @Test
        @DisplayName("should allow choice on the group deadline day itself (boundary: deadline not yet passed)")
        void shouldAllowChoiceOnDeadlineDay() {
            MembershipFeeGroup group = MembershipFeeGroup.reconstruct(
                    new MembershipFeeGroupId(UUID.randomUUID()),
                    LEVEL_ID_A,
                    "Deadline Today", YEAR, TODAY,
                    Money.ofCzk(new BigDecimal("1200.00")),
                    PublishedLevelStatus.EDITABLE,
                    List.of(), Set.of(), null);
            MembershipFeeGroupId groupId = group.getId();
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.of(buildOpenPublication()));
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.empty());
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.chooseFeeLevel(new MemberChoicePort.ChooseFeeLevel(MEMBER_ID, groupId, YEAR));

            verify(groupRepository).save(argThat(g -> g.hasMember(MEMBER_ID)));
        }

        @Test
        @DisplayName("should throw VotingClosedException the day after the group deadline")
        void shouldThrowDayAfterDeadline() {
            MembershipFeeGroup group = MembershipFeeGroup.reconstruct(
                    new MembershipFeeGroupId(UUID.randomUUID()),
                    LEVEL_ID_A,
                    "Deadline Yesterday", YEAR, TODAY.minusDays(1),
                    Money.ofCzk(new BigDecimal("1200.00")),
                    PublishedLevelStatus.EDITABLE,
                    List.of(), Set.of(), null);
            MembershipFeeGroupId groupId = group.getId();
            when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.of(buildOpenPublication()));

            assertThatThrownBy(() -> service.chooseFeeLevel(
                    new MemberChoicePort.ChooseFeeLevel(MEMBER_ID, groupId, YEAR)))
                    .isInstanceOf(VotingClosedException.class);
        }
    }

    @Nested
    @DisplayName("getRecommendedLevelForYear() — previous year default (D8)")
    class GetRecommendedLevelForYear {

        @Test
        @DisplayName("should return previous year's sourceLevelId when it is published in the requested year")
        void shouldReturnPreviousYearLevelId() {
            MembershipFeeGroup previousYearGroup = buildEditableGroup(LEVEL_ID_A);
            previousYearGroup.addMember(MEMBER_ID, LocalDate.of(YEAR - 1, 2, 1), AssignmentSource.MEMBER_CHOICE);

            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR - 1))
                    .thenReturn(Optional.of(previousYearGroup));
            when(groupRepository.existsByYearAndSourceLevelId(YEAR, LEVEL_ID_A)).thenReturn(true);

            Optional<MembershipFeeLevelId> result = service.getRecommendedLevelForYear(MEMBER_ID, YEAR);

            assertThat(result).contains(LEVEL_ID_A);
        }

        @Test
        @DisplayName("should return empty when member had no choice last year")
        void shouldReturnEmptyWhenNoLastYearChoice() {
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR - 1)).thenReturn(Optional.empty());

            Optional<MembershipFeeLevelId> result = service.getRecommendedLevelForYear(MEMBER_ID, YEAR);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when last year's level is not published for current year")
        void shouldReturnEmptyWhenLevelNotPublishedThisYear() {
            MembershipFeeGroup previousYearGroup = buildEditableGroup(LEVEL_ID_A);
            previousYearGroup.addMember(MEMBER_ID, LocalDate.of(YEAR - 1, 2, 1), AssignmentSource.MEMBER_CHOICE);

            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR - 1))
                    .thenReturn(Optional.of(previousYearGroup));
            when(groupRepository.existsByYearAndSourceLevelId(YEAR, LEVEL_ID_A)).thenReturn(false);

            Optional<MembershipFeeLevelId> result = service.getRecommendedLevelForYear(MEMBER_ID, YEAR);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCurrentChoice()")
    class GetCurrentChoice {

        @Test
        @DisplayName("should return group id when member has a choice")
        void shouldReturnGroupIdWhenChoiceExists() {
            MembershipFeeGroup group = buildEditableGroup(LEVEL_ID_A);
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.of(group));

            Optional<MembershipFeeGroupId> result = service.getCurrentChoice(MEMBER_ID, YEAR);

            assertThat(result).contains(group.getId());
        }

        @Test
        @DisplayName("should return empty when member has no choice")
        void shouldReturnEmptyWhenNoChoice() {
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.empty());

            Optional<MembershipFeeGroupId> result = service.getCurrentChoice(MEMBER_ID, YEAR);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("removeFeeChoice()")
    class RemoveFeeChoice {

        @Test
        @DisplayName("should remove member from their current group when voting is open")
        void shouldRemoveMemberFromGroup() {
            MembershipFeeGroup group = buildEditableGroup(LEVEL_ID_A);
            group.addMember(MEMBER_ID, LocalDate.of(YEAR, 1, 5), AssignmentSource.MEMBER_CHOICE);
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.of(group));
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.of(buildOpenPublication()));
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.removeFeeChoice(MEMBER_ID, YEAR);

            verify(groupRepository).save(argThat(g -> !g.hasMember(MEMBER_ID)));
        }

        @Test
        @DisplayName("should be no-op when member has no choice")
        void shouldBeNoOpWhenNoChoice() {
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.empty());
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.of(buildOpenPublication()));

            service.removeFeeChoice(MEMBER_ID, YEAR);

            verify(groupRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw VotingClosedException when removing after group voting deadline")
        void shouldThrowWhenRemovingAfterDeadline() {
            MembershipFeeGroup group = MembershipFeeGroup.reconstruct(
                    new MembershipFeeGroupId(UUID.randomUUID()),
                    LEVEL_ID_A,
                    "Expired Group", YEAR, TODAY.minusDays(1),
                    Money.ofCzk(new BigDecimal("1200.00")),
                    PublishedLevelStatus.EDITABLE,
                    List.of(),
                    new HashSet<>(Set.of(new FeeGroupMembership(
                            MEMBER_ID, LocalDate.of(YEAR, 1, 5),
                            AssignmentSource.MEMBER_CHOICE, null))),
                    null);
            when(publicationRepository.findByYear(YEAR)).thenReturn(Optional.of(buildOpenPublication()));
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.of(group));

            assertThatThrownBy(() -> service.removeFeeChoice(MEMBER_ID, YEAR))
                    .isInstanceOf(VotingClosedException.class);
        }
    }
}
