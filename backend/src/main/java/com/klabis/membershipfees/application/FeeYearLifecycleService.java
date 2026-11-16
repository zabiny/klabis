package com.klabis.membershipfees.application;

import com.klabis.finance.application.ChargePort;
import com.klabis.members.MemberId;
import com.klabis.members.application.AllMembersPort;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MemberFeeSelectionResolvedEvent;
import com.klabis.membershipfees.MemberMissedFeeSelectionEvent;
import com.klabis.membershipfees.domain.FeeGroupMembership;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import com.klabis.membershipfees.domain.FeeSelectionCampaignRepository;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import com.klabis.membershipfees.domain.PublishedLevelStatus;
import com.klabis.membershipfees.domain.YearlyFeeChargeMarkerRepository;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
class FeeYearLifecycleService implements FeeYearLifecyclePort {

    private static final Logger log = LoggerFactory.getLogger(FeeYearLifecycleService.class);

    private final FeeSelectionCampaignRepository campaignRepository;
    private final AllMembersPort allMembersPort;
    private final MembershipFeeGroupRepository groupRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ChargePort chargePort;
    private final YearlyFeeChargeMarkerRepository markerRepository;
    private final MemberFeePlacement memberFeePlacement;
    private final Clock clock;

    FeeYearLifecycleService(FeeSelectionCampaignRepository campaignRepository,
                            AllMembersPort allMembersPort,
                            MembershipFeeGroupRepository groupRepository,
                            ApplicationEventPublisher eventPublisher,
                            ChargePort chargePort,
                            YearlyFeeChargeMarkerRepository markerRepository,
                            Clock clock) {
        this.campaignRepository = campaignRepository;
        this.allMembersPort = allMembersPort;
        this.groupRepository = groupRepository;
        this.eventPublisher = eventPublisher;
        this.chargePort = chargePort;
        this.markerRepository = markerRepository;
        this.memberFeePlacement = new MemberFeePlacement(groupRepository);
        this.clock = clock;
    }

    @Transactional
    @Override
    public void closeCampaign(FeeSelectionCampaignId campaignId) {
        FeeSelectionCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new FeeSelectionCampaignNotFoundException(campaignId));

        if (campaign.isProcessed()) {
            throw new CampaignAlreadyProcessedException();
        }

        Set<MemberId> allMembers = allMembersPort.findAll();
        processCampaign(campaign, allMembers);
        campaignRepository.save(campaign);
    }

    @Transactional
    @Override
    public void processCampaignEnd(LocalDate toDate) {
        List<FeeSelectionCampaign> unprocessed = campaignRepository.findUnprocessedClosedPublications(toDate);
        if (unprocessed.isEmpty()) {
            return;
        }
        log.info("Processing missed fee selections for {} closed campaign(s)", unprocessed.size());

        Set<MemberId> allMembers = allMembersPort.findAll();

        for (FeeSelectionCampaign campaign : unprocessed) {
            processCampaign(campaign, allMembers);
            campaignRepository.save(campaign);
        }
    }

    @Transactional
    @Override
    public void assignLevel(AssignFeeLevel command) {
        MembershipFeeGroup targetGroup = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new MembershipFeeGroupNotFoundException(command.groupId()));

        FeeSelectionCampaign campaign = campaignRepository.findByYear(command.year())
                .orElseThrow(() -> new FeeSelectionCampaignNotFoundException(command.year()));

        LocalDate today = LocalDate.now(clock);
        memberFeePlacement.placeByAdminAssignment(
                new MemberFeePlacement.Move(targetGroup, command.targetMemberId(), command.year(), today),
                command.adminId());

        if (campaign.isClosed(today)) {
            chargeImmediately(command.targetMemberId(), command.year(), targetGroup);
        }

        // Always publish — the listener's unblockMember() is a no-op when the member is not blocked
        eventPublisher.publishEvent(new MemberFeeSelectionResolvedEvent(command.targetMemberId(), command.year()));
    }

    @Transactional
    @Override
    public void chooseFeeLevel(ChooseFeeLevel command) {
        MembershipFeeGroup targetGroup = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new MembershipFeeGroupNotFoundException(command.groupId()));

        requireCampaign(command.year());

        memberFeePlacement.placeByMemberChoice(new MemberFeePlacement.Move(
                targetGroup, command.memberId(), command.year(), LocalDate.now(clock)));
    }

    @Transactional
    @Override
    public void removeFeeChoice(MemberId memberId, int year) {
        requireCampaign(year);

        LocalDate today = LocalDate.now(clock);
        groupRepository.findByMemberAndYear(memberId, year).ifPresent(group -> {
            group.removeMemberChoice(memberId, today);
            groupRepository.save(group);
        });
    }

    private void requireCampaign(int year) {
        if (campaignRepository.findByYear(year).isEmpty()) {
            throw new FeeSelectionCampaignNotFoundException(year);
        }
    }

    private void chargeImmediately(MemberId memberId, int year, MembershipFeeGroup targetGroup) {
        if (markerRepository.existsByMemberIdAndYear(memberId, year)) {
            return;
        }
        chargePort.chargeMembershipFee(memberId, targetGroup.getYearlyFeeSnapshot().amount(), year);
        markerRepository.markCharged(memberId, year);
    }

    private void processCampaign(FeeSelectionCampaign campaign, Set<MemberId> allMembers) {
        int year = campaign.getYear();
        log.info("Processing missed selections for year {}", year);

        List<MembershipFeeGroup> groups = groupRepository.findByYear(year);

        freezeGroups(groups);

        chargeYearlyFees(year, groups);

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

        campaign.markProcessed(Instant.now());
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

    private void chargeYearlyFees(int year, List<MembershipFeeGroup> groups) {
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
