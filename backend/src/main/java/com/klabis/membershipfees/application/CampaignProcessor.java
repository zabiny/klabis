package com.klabis.membershipfees.application;

import com.klabis.finance.application.ChargePort;
import com.klabis.members.MemberId;
import com.klabis.membershipfees.MemberMissedFeeSelectionEvent;
import com.klabis.membershipfees.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
class CampaignProcessor {

    private static final Logger log = LoggerFactory.getLogger(CampaignProcessor.class);

    private final MembershipFeeGroupRepository groupRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ChargePort chargePort;
    private final YearlyFeeChargeMarkerRepository markerRepository;

    CampaignProcessor(MembershipFeeGroupRepository groupRepository, ApplicationEventPublisher eventPublisher,
                      ChargePort chargePort, YearlyFeeChargeMarkerRepository markerRepository) {
        this.groupRepository = groupRepository;
        this.eventPublisher = eventPublisher;
        this.chargePort = chargePort;
        this.markerRepository = markerRepository;
    }

    void processPublication(FeeSelectionCampaign publication, Set<MemberId> allMembers) {
        int year = publication.getYear();
        log.info("Processing missed selections for year {}", year);

        List<MembershipFeeGroup> groups = groupRepository.findByYear(year);

        freezeGroups(groups);

        chargeYearlyFees(publication, groups);

        Set<MemberId> membersWithChoice = groups.stream()
                .flatMap(group -> group.getMemberships().stream())
                .map(FeeGroupMembership::memberId)
                .collect(Collectors.toSet());

        log.info("Year {}: {} members total, {} with choice, {} without choice",
                year, allMembers.size(), membersWithChoice.size(), allMembers.size() - membersWithChoice.size());

        allMembers.stream()
                .filter(memberId -> !membersWithChoice.contains(memberId))
                .forEach(memberId -> {
                    log.warn("Member {} missed fee selection for year {} — publishing sanction event", memberId, year);
                    eventPublisher.publishEvent(new MemberMissedFeeSelectionEvent(memberId, year));
                });

        publication.markProcessed(Instant.now());
    }

    private void freezeGroups(List<MembershipFeeGroup> groups) {
        for (MembershipFeeGroup group : groups) {
            if (group.getStatus() != PublishedLevelStatus.FROZEN) {
                group.freeze();
                groupRepository.save(group);
                log.info("Froze MembershipFeeGroup {}", group.getId());
            }
        }
    }

    private void chargeYearlyFees(FeeSelectionCampaign publication, List<MembershipFeeGroup> groups) {
        int year = publication.getYear();
        Set<MemberId> alreadyCharged = markerRepository.findChargedMemberIdsForYear(year);

        for (MembershipFeeGroup group : groups) {
            for (FeeGroupMembership membership : group.getMemberships()) {
                MemberId memberId = membership.memberId();
                if (alreadyCharged.contains(memberId)) {
                    log.debug("Skipping yearly fee charge for member {} year {} — already charged", memberId, year);
                    continue;
                }
                chargePort.chargeMembershipFee(memberId, group.getYearlyFeeSnapshot().amount(), year);
                markerRepository.markCharged(memberId, year);
                log.info("Charged yearly fee for member {} year {}", memberId, year);
            }
        }
    }

}
