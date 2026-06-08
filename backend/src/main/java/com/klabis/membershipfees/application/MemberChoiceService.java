package com.klabis.membershipfees.application;

import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.membershipfees.domain.FeeYearPublication;
import com.klabis.membershipfees.domain.FeeYearPublicationRepository;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import com.klabis.membershipfees.domain.VotingClosedException;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
class MemberChoiceService implements MemberChoicePort {

    private final MembershipFeeGroupRepository groupRepository;
    private final FeeYearPublicationRepository publicationRepository;

    MemberChoiceService(MembershipFeeGroupRepository groupRepository,
                        FeeYearPublicationRepository publicationRepository) {
        this.groupRepository = groupRepository;
        this.publicationRepository = publicationRepository;
    }

    @Transactional
    @Override
    public void chooseFeeLevel(ChooseFeeLevel command) {
        MembershipFeeGroup targetGroup = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new MembershipFeeGroupNotFoundException(command.groupId()));

        FeeYearPublication publication = publicationRepository.findByYear(command.year())
                .orElseThrow(() -> new FeeYearPublicationNotFoundException(command.year()));

        LocalDate today = LocalDate.now();
        if (publication.isClosed(today)) {
            throw new VotingClosedException();
        }

        groupRepository.findByMemberAndYear(command.memberId(), command.year())
                .filter(currentGroup -> !currentGroup.getId().equals(command.groupId()))
                .ifPresent(currentGroup -> {
                    currentGroup.removeMember(command.memberId());
                    groupRepository.save(currentGroup);
                });

        targetGroup.addMember(command.memberId(), today, AssignmentSource.MEMBER_CHOICE);
        groupRepository.save(targetGroup);
    }

    @Transactional
    @Override
    public void removeFeeChoice(MemberId memberId, int year) {
        FeeYearPublication publication = publicationRepository.findByYear(year)
                .orElseThrow(() -> new FeeYearPublicationNotFoundException(year));

        if (publication.isClosed(LocalDate.now())) {
            throw new VotingClosedException();
        }

        groupRepository.findByMemberAndYear(memberId, year).ifPresent(group -> {
            group.removeMember(memberId);
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
                .filter(lastYearLevelId -> isLevelPublishedForYear(lastYearLevelId, year));
    }

    private boolean isLevelPublishedForYear(MembershipFeeLevelId levelId, int year) {
        List<MembershipFeeGroup> currentYearGroups = groupRepository.findByYear(year);
        return currentYearGroups.stream()
                .anyMatch(group -> group.getSourceLevelId().equals(levelId));
    }
}
