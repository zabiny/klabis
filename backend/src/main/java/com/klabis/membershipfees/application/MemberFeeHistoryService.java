package com.klabis.membershipfees.application;

import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.domain.FeeYearPublicationRepository;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
class MemberFeeHistoryService implements MemberFeeHistoryPort {

    private final MembershipFeeGroupRepository groupRepository;
    private final FeeYearPublicationRepository publicationRepository;

    MemberFeeHistoryService(MembershipFeeGroupRepository groupRepository,
                             FeeYearPublicationRepository publicationRepository) {
        this.groupRepository = groupRepository;
        this.publicationRepository = publicationRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public CurrentLevelInfo getCurrentLevelInfo(MemberId memberId, int year) {
        Optional<MembershipFeeGroup> currentGroup = groupRepository.findByMemberAndYear(memberId, year);
        boolean votingOpen = publicationRepository.findByYear(year)
                .map(pub -> !pub.isClosed(LocalDate.now()))
                .orElse(false);

        if (currentGroup.isPresent()) {
            MembershipFeeGroup group = currentGroup.get();
            return new CurrentLevelInfo(
                    group.getId(), group.getName(), group.getYearlyFeeSnapshot(),
                    votingOpen, Optional.empty());
        }

        Optional<MembershipFeeLevelId> recommended = votingOpen
                ? resolveRecommendedLevel(memberId, year)
                : Optional.empty();

        return new CurrentLevelInfo(null, null, null, votingOpen, recommended);
    }

    @Transactional(readOnly = true)
    @Override
    public List<LevelAssignment> getLevelHistory(MemberId memberId) {
        return groupRepository.findByMember(memberId).stream()
                .sorted(Comparator.comparingInt(MembershipFeeGroup::getYear).reversed())
                .map(group -> toAssignment(memberId, group))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<LevelAssignment> toAssignment(MemberId memberId, MembershipFeeGroup group) {
        return group.getMemberships().stream()
                .filter(m -> m.memberId().equals(memberId))
                .findFirst()
                .map(m -> new LevelAssignment(group.getYear(), group.getId(), group.getName(),
                        m.joinedAt(), m.source()));
    }

    private Optional<MembershipFeeLevelId> resolveRecommendedLevel(MemberId memberId, int year) {
        return groupRepository.findByMemberAndYear(memberId, year - 1)
                .map(MembershipFeeGroup::getSourceLevelId)
                .filter(lastYearLevelId -> groupRepository.existsByYearAndSourceLevelId(year, lastYearLevelId));
    }
}
