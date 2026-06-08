package com.klabis.membershipfees.infrastructure.scheduler;

import com.klabis.common.users.UserId;
import com.klabis.finance.application.ChargePort;
import com.klabis.membershipfees.MemberMissedFeeSelectionEvent;
import com.klabis.membershipfees.domain.FeeGroupMembership;
import com.klabis.membershipfees.domain.FeeYearPublication;
import com.klabis.membershipfees.domain.FeeYearPublicationRepository;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import com.klabis.membershipfees.domain.PublishedLevelStatus;
import com.klabis.membershipfees.domain.YearlyFeeChargeMarkerRepository;
import com.klabis.members.MemberId;
import com.klabis.members.application.AllMembersPort;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
class FeeSelectionDeadlineScheduler {

    private static final Logger log = LoggerFactory.getLogger(FeeSelectionDeadlineScheduler.class);

    static final UserId SYSTEM_USER_ID = new UserId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

    private final FeeYearPublicationRepository publicationRepository;
    private final MembershipFeeGroupRepository groupRepository;
    private final AllMembersPort allMembersPort;
    private final ApplicationEventPublisher eventPublisher;
    private final ChargePort chargePort;
    private final YearlyFeeChargeMarkerRepository markerRepository;

    FeeSelectionDeadlineScheduler(FeeYearPublicationRepository publicationRepository,
                                   MembershipFeeGroupRepository groupRepository,
                                   AllMembersPort allMembersPort,
                                   ApplicationEventPublisher eventPublisher,
                                   ChargePort chargePort,
                                   YearlyFeeChargeMarkerRepository markerRepository) {
        this.publicationRepository = publicationRepository;
        this.groupRepository = groupRepository;
        this.allMembersPort = allMembersPort;
        this.eventPublisher = eventPublisher;
        this.chargePort = chargePort;
        this.markerRepository = markerRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    void processMissedSelections() {
        processMissedSelections(LocalDate.now());
    }

    @Transactional
    void processMissedSelections(LocalDate today) {
        List<FeeYearPublication> unprocessed = publicationRepository.findUnprocessedClosedPublications(today);
        if (unprocessed.isEmpty()) {
            return;
        }
        log.info("Processing missed fee selections for {} closed publication(s)", unprocessed.size());

        Set<MemberId> allMembers = allMembersPort.findAll();

        for (FeeYearPublication publication : unprocessed) {
            processPublication(publication, allMembers);
        }
    }

    private void processPublication(FeeYearPublication publication, Set<MemberId> allMembers) {
        int year = publication.getYear();
        log.info("Processing missed selections for year {}", year);

        List<MembershipFeeGroup> groups = groupRepository.findByYear(year);

        freezeGroups(groups);

        chargeYearlyFees(publication, groups);

        Set<MemberId> membersWithChoice = groups.stream()
                .flatMap(group -> group.getMemberships().stream())
                .map(FeeGroupMembership::memberId)
                .collect(Collectors.toSet());

        Set<MemberId> membersWithoutChoice = allMembers.stream()
                .filter(memberId -> !membersWithChoice.contains(memberId))
                .collect(Collectors.toSet());

        log.info("Year {}: {} members total, {} with choice, {} without choice",
                year, allMembers.size(), membersWithChoice.size(), membersWithoutChoice.size());

        membersWithoutChoice.forEach(memberId -> {
            log.warn("Member {} missed fee selection for year {} — publishing sanction event", memberId, year);
            eventPublisher.publishEvent(new MemberMissedFeeSelectionEvent(memberId, year));
        });

        publication.markProcessed(Instant.now());
        publicationRepository.save(publication);
    }

    private void chargeYearlyFees(FeeYearPublication publication, List<MembershipFeeGroup> groups) {
        int year = publication.getYear();
        LocalDate occurredAt = publication.getVotingDeadline().plusDays(1);
        String note = "Roční členský příspěvek " + year;

        Set<MemberId> alreadyCharged = markerRepository.findChargedMemberIdsForYear(year);

        for (MembershipFeeGroup group : groups) {
            for (FeeGroupMembership membership : group.getMemberships()) {
                MemberId memberId = membership.memberId();
                if (alreadyCharged.contains(memberId)) {
                    log.debug("Skipping yearly fee charge for member {} year {} — already charged", memberId, year);
                    continue;
                }
                chargePort.charge(new ChargePort.ChargeCommand(
                        memberId,
                        group.getYearlyFeeSnapshot().amount(),
                        occurredAt,
                        note,
                        SYSTEM_USER_ID));
                markerRepository.markCharged(memberId, year);
                log.info("Charged yearly fee for member {} year {}", memberId, year);
            }
        }
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
}
