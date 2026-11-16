package com.klabis.membershipfees.application;

import com.klabis.finance.domain.Money;
import com.klabis.members.MemberId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.membershipfees.domain.FeeGroupMembership;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import com.klabis.membershipfees.domain.PublishedLevelStatus;
import com.klabis.membershipfees.domain.VotingClosedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberFeePlacement")
class MemberFeePlacementTest {

    private static final MemberId MEMBER_ID = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MemberId ADMIN_ID = new MemberId(UUID.fromString("a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0"));
    private static final MembershipFeeTierId LEVEL_ID_A =
            new MembershipFeeTierId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MembershipFeeTierId LEVEL_ID_B =
            new MembershipFeeTierId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final int YEAR = 2026;
    private static final LocalDate TODAY = LocalDate.of(YEAR, 3, 15);
    private static final LocalDate OPEN_DEADLINE = LocalDate.of(YEAR, 3, 31);
    private static final LocalDate PASSED_DEADLINE = TODAY.minusDays(1);

    @Mock
    private MembershipFeeGroupRepository groupRepository;

    private MemberFeePlacement testedInstance;

    @BeforeEach
    void setUp() {
        testedInstance = new MemberFeePlacement(groupRepository);
    }

    private MembershipFeeGroup buildGroup(MembershipFeeTierId levelId, String name,
                                          LocalDate votingDeadline, PublishedLevelStatus status,
                                          Set<FeeGroupMembership> memberships) {
        return MembershipFeeGroup.reconstruct(
                new MembershipFeeGroupId(UUID.randomUUID()),
                levelId, name, YEAR, votingDeadline,
                Money.ofCzk(new BigDecimal("1200.00")),
                status, List.of(), memberships, null);
    }

    private static Set<FeeGroupMembership> membershipOf(MemberId memberId, AssignmentSource source) {
        return Set.of(new FeeGroupMembership(memberId, LocalDate.of(YEAR, 1, 5), source, null));
    }

    @Nested
    @DisplayName("placeByMemberChoice()")
    class PlaceByMemberChoice {

        @Test
        @DisplayName("should add the member to the target group as a member choice")
        void shouldAddMemberToTargetGroup() {
            MembershipFeeGroup targetGroup = buildGroup(LEVEL_ID_B, "New", OPEN_DEADLINE,
                    PublishedLevelStatus.EDITABLE, Set.of());
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.empty());

            testedInstance.placeByMemberChoice(
                    new MemberFeePlacement.Move(targetGroup, MEMBER_ID, YEAR, TODAY));

            assertThat(targetGroup.hasMember(MEMBER_ID)).isTrue();
            FeeGroupMembership membership = targetGroup.getMemberships().iterator().next();
            assertThat(membership.source()).isEqualTo(AssignmentSource.MEMBER_CHOICE);
            assertThat(membership.assignedBy()).isNull();
            verify(groupRepository).save(targetGroup);
        }

        @Test
        @DisplayName("should remove the member from the current group and save both groups")
        void shouldRemoveMemberFromCurrentGroupAndSaveBothGroups() {
            MembershipFeeGroup currentGroup = buildGroup(LEVEL_ID_A, "Old", OPEN_DEADLINE,
                    PublishedLevelStatus.EDITABLE, membershipOf(MEMBER_ID, AssignmentSource.MEMBER_CHOICE));
            MembershipFeeGroupId currentGroupId = currentGroup.getId();
            MembershipFeeGroup targetGroup = buildGroup(LEVEL_ID_B, "New", OPEN_DEADLINE,
                    PublishedLevelStatus.EDITABLE, Set.of());
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.of(currentGroup));

            testedInstance.placeByMemberChoice(
                    new MemberFeePlacement.Move(targetGroup, MEMBER_ID, YEAR, TODAY));

            assertThat(currentGroup.hasMember(MEMBER_ID)).isFalse();
            assertThat(targetGroup.hasMember(MEMBER_ID)).isTrue();
            verify(groupRepository).save(argThat(saved -> saved.getId().equals(currentGroupId)
                    && !saved.hasMember(MEMBER_ID)));
            verify(groupRepository).save(argThat(saved -> saved.getId().equals(targetGroup.getId())
                    && saved.hasMember(MEMBER_ID)));
        }

        @Test
        @DisplayName("should keep the voting-closed guard when removing from a frozen current group")
        void shouldRejectRemovalWhenVotingIsClosed() {
            MembershipFeeGroup currentGroup = buildGroup(LEVEL_ID_A, "Frozen", PASSED_DEADLINE,
                    PublishedLevelStatus.FROZEN, membershipOf(MEMBER_ID, AssignmentSource.MEMBER_CHOICE));
            MembershipFeeGroup targetGroup = buildGroup(LEVEL_ID_B, "New", OPEN_DEADLINE,
                    PublishedLevelStatus.EDITABLE, Set.of());
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.of(currentGroup));

            assertThatThrownBy(() -> testedInstance.placeByMemberChoice(
                    new MemberFeePlacement.Move(targetGroup, MEMBER_ID, YEAR, TODAY)))
                    .isInstanceOf(VotingClosedException.class);

            assertThat(currentGroup.hasMember(MEMBER_ID)).isTrue();
            verify(groupRepository, never()).save(any());
        }

        @Test
        @DisplayName("should only add to the target group when the member has no current group")
        void shouldOnlyAddWhenNoCurrentGroup() {
            MembershipFeeGroup targetGroup = buildGroup(LEVEL_ID_B, "New", OPEN_DEADLINE,
                    PublishedLevelStatus.EDITABLE, Set.of());
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.empty());

            testedInstance.placeByMemberChoice(
                    new MemberFeePlacement.Move(targetGroup, MEMBER_ID, YEAR, TODAY));

            verify(groupRepository, times(1)).save(targetGroup);
        }

        @Test
        @DisplayName("should touch no other group when the member already belongs to the target group")
        void shouldNotTouchOtherGroupsWhenAlreadyInTargetGroup() {
            MembershipFeeGroup targetGroup = buildGroup(LEVEL_ID_B, "New", OPEN_DEADLINE,
                    PublishedLevelStatus.EDITABLE, membershipOf(MEMBER_ID, AssignmentSource.MEMBER_CHOICE));
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.of(targetGroup));

            testedInstance.placeByMemberChoice(
                    new MemberFeePlacement.Move(targetGroup, MEMBER_ID, YEAR, TODAY));

            assertThat(targetGroup.memberCount()).isEqualTo(1);
            verify(groupRepository, times(1)).save(targetGroup);
        }
    }

    @Nested
    @DisplayName("placeByAdminAssignment()")
    class PlaceByAdminAssignment {

        @Test
        @DisplayName("should add the member to the target group as an administrator assignment recording the administrator")
        void shouldAddMemberAsAdminAssignment() {
            MembershipFeeGroup targetGroup = buildGroup(LEVEL_ID_B, "New", OPEN_DEADLINE,
                    PublishedLevelStatus.EDITABLE, Set.of());
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.empty());

            testedInstance.placeByAdminAssignment(
                    new MemberFeePlacement.Move(targetGroup, MEMBER_ID, YEAR, TODAY), ADMIN_ID);

            FeeGroupMembership membership = targetGroup.getMemberships().iterator().next();
            assertThat(membership.source()).isEqualTo(AssignmentSource.ADMIN_ASSIGNMENT);
            assertThat(membership.assignedBy()).isEqualTo(ADMIN_ID);
            verify(groupRepository).save(targetGroup);
        }

        @Test
        @DisplayName("should ignore the voting window when removing from a frozen current group and save both groups")
        void shouldRemoveMemberFromFrozenCurrentGroupWithoutGuard() {
            MembershipFeeGroup currentGroup = buildGroup(LEVEL_ID_A, "Frozen", PASSED_DEADLINE,
                    PublishedLevelStatus.FROZEN, membershipOf(MEMBER_ID, AssignmentSource.MEMBER_CHOICE));
            MembershipFeeGroupId currentGroupId = currentGroup.getId();
            MembershipFeeGroup targetGroup = buildGroup(LEVEL_ID_B, "New", OPEN_DEADLINE,
                    PublishedLevelStatus.EDITABLE, Set.of());
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.of(currentGroup));

            testedInstance.placeByAdminAssignment(
                    new MemberFeePlacement.Move(targetGroup, MEMBER_ID, YEAR, TODAY), ADMIN_ID);

            assertThat(currentGroup.hasMember(MEMBER_ID)).isFalse();
            assertThat(targetGroup.hasMember(MEMBER_ID)).isTrue();
            verify(groupRepository).save(argThat(saved -> saved.getId().equals(currentGroupId)
                    && !saved.hasMember(MEMBER_ID)));
            verify(groupRepository).save(argThat(saved -> saved.getId().equals(targetGroup.getId())
                    && saved.hasMember(MEMBER_ID)));
        }

        @Test
        @DisplayName("should only add to the target group when the member has no current group")
        void shouldOnlyAddWhenNoCurrentGroup() {
            MembershipFeeGroup targetGroup = buildGroup(LEVEL_ID_B, "New", OPEN_DEADLINE,
                    PublishedLevelStatus.EDITABLE, Set.of());
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.empty());

            testedInstance.placeByAdminAssignment(
                    new MemberFeePlacement.Move(targetGroup, MEMBER_ID, YEAR, TODAY), ADMIN_ID);

            verify(groupRepository, times(1)).save(targetGroup);
        }

        @Test
        @DisplayName("should touch no other group when the member already belongs to the target group")
        void shouldNotTouchOtherGroupsWhenAlreadyInTargetGroup() {
            MembershipFeeGroup targetGroup = buildGroup(LEVEL_ID_B, "New", OPEN_DEADLINE,
                    PublishedLevelStatus.EDITABLE, membershipOf(MEMBER_ID, AssignmentSource.ADMIN_ASSIGNMENT));
            when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR)).thenReturn(Optional.of(targetGroup));

            testedInstance.placeByAdminAssignment(
                    new MemberFeePlacement.Move(targetGroup, MEMBER_ID, YEAR, TODAY), ADMIN_ID);

            assertThat(targetGroup.memberCount()).isEqualTo(1);
            verify(groupRepository, times(1)).save(targetGroup);
        }
    }
}
