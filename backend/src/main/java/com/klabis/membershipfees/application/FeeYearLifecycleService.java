package com.klabis.membershipfees.application;

import com.klabis.finance.application.ChargePort;
import com.klabis.members.MemberId;
import com.klabis.members.application.AllMembersPort;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MemberFeeSelectionResolvedEvent;
import com.klabis.membershipfees.MemberMissedFeeSelectionEvent;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.domain.AssignmentSource;
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
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Clock;
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
        MembershipFeeGroup targetGroup = requireGroup(command.groupId());
        requireYearMatchesGroup(command.year(), targetGroup);

        FeeSelectionCampaign campaign = requireCampaign(command.year());

        LocalDate today = LocalDate.now(clock);
        memberFeePlacement.place(
                new MemberFeePlacement.Move(targetGroup, command.targetMemberId(), today),
                AssignmentSource.ADMIN_ASSIGNMENT,
                command.adminId());

        if (campaign.isClosed(today)) {
            chargeOnce(command.targetMemberId(), command.year(), targetGroup.getYearlyFeeSnapshot().amount());
        }

        // Always publish — the listener's unblockMember() is a no-op when the member is not blocked
        eventPublisher.publishEvent(new MemberFeeSelectionResolvedEvent(command.targetMemberId(), command.year()));
    }

    @Transactional
    @Override
    public void chooseFeeLevel(ChooseFeeLevel command) {
        MembershipFeeGroup targetGroup = requireGroup(command.groupId());
        requireYearMatchesGroup(command.year(), targetGroup);

        requireCampaign(command.year());

        memberFeePlacement.place(
                new MemberFeePlacement.Move(targetGroup, command.memberId(), LocalDate.now(clock)),
                AssignmentSource.MEMBER_CHOICE,
                null);
    }

    @Transactional
    @Override
    public void removeFeeChoice(MemberId memberId, int year) {
        requireCampaign(year);

        memberFeePlacement.withdraw(memberId, year, LocalDate.now(clock));
    }

    private MembershipFeeGroup requireGroup(MembershipFeeGroupId id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new MembershipFeeGroupNotFoundException(id));
    }

    private FeeSelectionCampaign requireCampaign(int year) {
        return campaignRepository.findByYear(year)
                .orElseThrow(() -> new FeeSelectionCampaignNotFoundException(year));
    }

    private void requireYearMatchesGroup(int year, MembershipFeeGroup group) {
        Assert.isTrue(year == group.getYear(),
                "Requested year " + year + " does not match the year " + group.getYear() + " of the fee group");
    }

    private void chargeOnce(MemberId memberId, int year, BigDecimal amount) {
        if (markerRepository.existsByMemberIdAndYear(memberId, year)) {
            log.debug("Skipping yearly fee charge for member {} year {} — already charged", memberId, year);
            return;
        }
        chargePort.chargeMembershipFee(memberId, amount, year);
        markerRepository.markCharged(memberId, year);
        log.info("Charged yearly fee for member {} year {}", memberId, year);
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

        campaign.markProcessed(clock.instant());
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
        for (MembershipFeeGroup group : groups) {
            for (FeeGroupMembership membership : group.getMemberships()) {
                chargeOnce(membership.memberId(), year, group.getYearlyFeeSnapshot().amount());
            }
        }
    }
}
