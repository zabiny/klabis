package com.klabis.membershipfees.application;

import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.membershipfees.domain.FeeYearPublicationRepository;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

@Service
class MemberChoiceService implements MemberChoicePort {

    private final MembershipFeeGroupRepository groupRepository;
    private final FeeYearPublicationRepository publicationRepository;
    private final Clock clock;

    MemberChoiceService(MembershipFeeGroupRepository groupRepository,
                        FeeYearPublicationRepository publicationRepository,
                        Clock clock) {
        this.groupRepository = groupRepository;
        this.publicationRepository = publicationRepository;
        this.clock = clock;
    }

    @Transactional
    @Override
    public void chooseFeeLevel(ChooseFeeLevel command) {
        MembershipFeeGroup targetGroup = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new MembershipFeeGroupNotFoundException(command.groupId()));

        if (publicationRepository.findByYear(command.year()).isEmpty()) {
            throw new FeeYearPublicationNotFoundException(command.year());
        }

        LocalDate today = LocalDate.now(clock);

        groupRepository.findByMemberAndYear(command.memberId(), command.year())
                .filter(currentGroup -> !currentGroup.getId().equals(command.groupId()))
                .ifPresent(currentGroup -> {
                    currentGroup.removeMemberChoice(command.memberId(), today);
                    groupRepository.save(currentGroup);
                });

        targetGroup.addMember(command.memberId(), today, AssignmentSource.MEMBER_CHOICE);
        groupRepository.save(targetGroup);
    }

    @Transactional
    @Override
    public void removeFeeChoice(MemberId memberId, int year) {
        if (publicationRepository.findByYear(year).isEmpty()) {
            throw new FeeYearPublicationNotFoundException(year);
        }

        LocalDate today = LocalDate.now(clock);
        groupRepository.findByMemberAndYear(memberId, year).ifPresent(group -> {
            group.removeMemberChoice(memberId, today);
            groupRepository.save(group);
        });
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<MembershipFeeGroupId> getCurrentChoice(MemberId memberId, int year) {
        return groupRepository.findByMemberAndYear(memberId, year)
                .map(MembershipFeeGroup::getId);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<MembershipFeeLevelId> getRecommendedLevelForYear(MemberId memberId, int year) {
        return groupRepository.findByMemberAndYear(memberId, year - 1)
                .map(MembershipFeeGroup::getSourceLevelId)
                .filter(lastYearLevelId -> groupRepository.existsByYearAndSourceLevelId(year, lastYearLevelId));
    }
}
