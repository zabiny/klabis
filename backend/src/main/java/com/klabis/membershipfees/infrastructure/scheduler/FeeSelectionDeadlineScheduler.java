package com.klabis.membershipfees.infrastructure.scheduler;

import com.klabis.membershipfees.MemberMissedFeeSelectionEvent;
import com.klabis.membershipfees.domain.FeeYearPublication;
import com.klabis.membershipfees.domain.FeeYearPublicationRepository;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import com.klabis.membershipfees.domain.PublishedLevelStatus;
import com.klabis.members.MemberId;
import com.klabis.members.application.AllMembersPort;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
class FeeSelectionDeadlineScheduler {

    private static final Logger log = LoggerFactory.getLogger(FeeSelectionDeadlineScheduler.class);

    private final FeeYearPublicationRepository publicationRepository;
    private final MembershipFeeGroupRepository groupRepository;
    private final AllMembersPort allMembersPort;
    private final ApplicationEventPublisher eventPublisher;

    FeeSelectionDeadlineScheduler(FeeYearPublicationRepository publicationRepository,
                                   MembershipFeeGroupRepository groupRepository,
                                   AllMembersPort allMembersPort,
                                   ApplicationEventPublisher eventPublisher) {
        this.publicationRepository = publicationRepository;
        this.groupRepository = groupRepository;
        this.allMembersPort = allMembersPort;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 0 3 * * *")
    void processMissedSelections() {
        processMissedSelections(LocalDate.now());
    }

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

        Set<MemberId> membersWithChoice = groups.stream()
                .flatMap(group -> group.getMemberships().stream())
                .map(membership -> membership.memberId())
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
