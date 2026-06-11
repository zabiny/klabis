package com.klabis.membershipfees.application;

import com.klabis.members.MemberId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.domain.FeeSelectionCampaignRepository;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
class MemberFeeHistoryService implements MemberFeeHistoryPort {

    private final MembershipFeeGroupRepository groupRepository;
    private final FeeSelectionCampaignRepository publicationRepository;
    private final MemberChoicePort memberChoicePort;
    private final Clock clock;

    MemberFeeHistoryService(MembershipFeeGroupRepository groupRepository,
                             FeeSelectionCampaignRepository publicationRepository,
                             MemberChoicePort memberChoicePort,
                             Clock clock) {
        this.groupRepository = groupRepository;
        this.publicationRepository = publicationRepository;
        this.memberChoicePort = memberChoicePort;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    @Override
    public CurrentLevelInfo getCurrentLevelInfo(MemberId memberId, int year) {
        Optional<MembershipFeeGroup> currentGroup = groupRepository.findByMemberAndYear(memberId, year);
        boolean votingOpen = publicationRepository.findByYear(year)
                .map(pub -> !pub.isClosed(LocalDate.now(clock)))
                .orElse(false);

        if (currentGroup.isPresent()) {
            MembershipFeeGroup group = currentGroup.get();
            return new CurrentLevelInfo(
                    group.getId(), group.getName(), group.getYearlyFeeSnapshot(),
                    votingOpen, Optional.empty());
        }

        Optional<MembershipFeeTierId> recommended = votingOpen
                ? memberChoicePort.getRecommendedLevelForYear(memberId, year)
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

}
