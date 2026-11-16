package com.klabis.membershipfees.application;

import com.klabis.members.MemberId;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.time.LocalDate;

/**
 * Moves a member into the target group's year, taking the member out of whichever group of that same
 * year currently holds them. A member choice leaves that group through the voting-window guard while
 * an administrator assignment overrides it — the asymmetry required by
 * "Emergency Assignment by Administrator" and "Member Chooses a Fee Level for the Upcoming Year".
 */
class MemberFeePlacement {

    record Move(MembershipFeeGroup targetGroup, MemberId memberId, LocalDate today) {
        Move {
            Assert.notNull(targetGroup, "TargetGroup is required");
            Assert.notNull(memberId, "MemberId is required");
            Assert.notNull(today, "Today is required");
        }

        int year() {
            return targetGroup.getYear();
        }
    }

    private final MembershipFeeGroupRepository groupRepository;

    MemberFeePlacement(MembershipFeeGroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    void place(Move move, AssignmentSource source, @Nullable MemberId assignedBy) {
        leaveCurrentGroup(move, source);
        move.targetGroup().addMember(move.memberId(), move.today(), source, assignedBy);
        groupRepository.save(move.targetGroup());
    }

    void withdraw(MemberId memberId, int year, LocalDate today) {
        groupRepository.findByMemberAndYear(memberId, year).ifPresent(currentGroup -> {
            currentGroup.removeMemberChoice(memberId, today);
            groupRepository.save(currentGroup);
        });
    }

    private void leaveCurrentGroup(Move move, AssignmentSource source) {
        groupRepository.findByMemberAndYear(move.memberId(), move.year())
                .filter(currentGroup -> !currentGroup.getId().equals(move.targetGroup().getId()))
                .ifPresent(currentGroup -> {
                    if (source == AssignmentSource.MEMBER_CHOICE) {
                        currentGroup.removeMemberChoice(move.memberId(), move.today());
                    } else {
                        currentGroup.removeMember(move.memberId());
                    }
                    groupRepository.save(currentGroup);
                });
    }
}
