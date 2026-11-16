package com.klabis.membershipfees.application;

import com.klabis.members.MemberId;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * Moves a member into a fee group of a given year, taking the member out of whichever group of that
 * year currently holds them. A member choice leaves that group through the voting-window guard while
 * an administrator assignment overrides it — the asymmetry required by
 * "Emergency Assignment by Administrator" and "Member Chooses a Fee Level for the Upcoming Year".
 */
class MemberFeePlacement {

    record Move(MembershipFeeGroup targetGroup, MemberId memberId, int year, LocalDate today) {
        Move {
            Assert.notNull(targetGroup, "TargetGroup is required");
            Assert.notNull(memberId, "MemberId is required");
            Assert.notNull(today, "Today is required");
        }
    }

    private final MembershipFeeGroupRepository groupRepository;

    MemberFeePlacement(MembershipFeeGroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    void placeByMemberChoice(Move move) {
        leaveCurrentGroup(move, currentGroup -> currentGroup.removeMemberChoice(move.memberId(), move.today()));
        move.targetGroup().addMember(move.memberId(), move.today(), AssignmentSource.MEMBER_CHOICE);
        groupRepository.save(move.targetGroup());
    }

    void placeByAdminAssignment(Move move, MemberId assignedBy) {
        leaveCurrentGroup(move, currentGroup -> currentGroup.removeMember(move.memberId()));
        move.targetGroup().addMember(move.memberId(), move.today(), AssignmentSource.ADMIN_ASSIGNMENT, assignedBy);
        groupRepository.save(move.targetGroup());
    }

    private void leaveCurrentGroup(Move move, Consumer<MembershipFeeGroup> removal) {
        groupRepository.findByMemberAndYear(move.memberId(), move.year())
                .filter(currentGroup -> !currentGroup.getId().equals(move.targetGroup().getId()))
                .ifPresent(currentGroup -> {
                    removal.accept(currentGroup);
                    groupRepository.save(currentGroup);
                });
    }
}
