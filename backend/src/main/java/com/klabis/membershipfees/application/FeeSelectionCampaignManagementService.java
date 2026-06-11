package com.klabis.membershipfees.application;

import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
class FeeSelectionCampaignManagementService implements FeeSelectionCampaignManagementPort {

    private final FeeSelectionCampaignRepository publicationRepository;
    private final MembershipFeeGroupRepository groupRepository;
    private final MembershipFeeTierRepository tierRepository;
    private final Clock clock;

    FeeSelectionCampaignManagementService(FeeSelectionCampaignRepository publicationRepository,
                                          MembershipFeeGroupRepository groupRepository,
                                          MembershipFeeTierRepository tierRepository,
                                          Clock clock) {
        this.publicationRepository = publicationRepository;
        this.groupRepository = groupRepository;
        this.tierRepository = tierRepository;
        this.clock = clock;
    }

    @Transactional
    @Override
    public FeeSelectionCampaignId publishYear(PublishYearCommand command) {
        LocalDate today = LocalDate.now(clock);
        if (!command.votingDeadline().isAfter(today)) {
            throw new DeadlineNotInFutureException(command.votingDeadline());
        }
        publicationRepository.findActive(today).ifPresent(existing -> {
            throw new ActiveCampaignExistsException();
        });

        List<MembershipFeeTier> levels = command.levelIds().stream()
                .map(id -> tierRepository.findById(id)
                        .orElseThrow(() -> new MembershipFeeTierNotFoundException(id)))
                .toList();

        FeeSelectionCampaign.FeeSelectionCampaignWithGroups result = FeeSelectionCampaign.publish(
                command.year(), command.votingDeadline(), levels);

        for (MembershipFeeGroup group : result.groups()) {
            groupRepository.save(group);
        }

        return publicationRepository.save(result.publication()).getId();
    }

    @Transactional(readOnly = true)
    @Override
    public FeeSelectionCampaign getPublication(FeeSelectionCampaignId id) {
        return publicationRepository.findById(id)
                .orElseThrow(() -> new FeeSelectionCampaignNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<FeeSelectionCampaign> getPublicationForYear(int year) {
        return publicationRepository.findByYear(year);
    }

    @Transactional(readOnly = true)
    @Override
    public List<FeeSelectionCampaign> listPublications() {
        return publicationRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Override
    public List<MembershipFeeGroup> listGroupsForYear(int year) {
        return groupRepository.findByYear(year);
    }

    @Transactional(readOnly = true)
    @Override
    public MembershipFeeGroup getGroup(MembershipFeeGroupId id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new MembershipFeeGroupNotFoundException(id));
    }

    @Transactional
    @Override
    public FeeSelectionCampaign changeDeadline(FeeSelectionCampaignId id, ChangeDeadlineCommand command) {
        FeeSelectionCampaign campaign = publicationRepository.findById(id)
                .orElseThrow(() -> new FeeSelectionCampaignNotFoundException(id));
        LocalDate today = LocalDate.now(clock);
        campaign.changeDeadline(command.votingDeadline(), today);
        return publicationRepository.save(campaign);
    }

    @Transactional
    @Override
    public void editGroupSnapshot(MembershipFeeGroupId id, EditGroupSnapshotCommand command) {
        MembershipFeeGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new MembershipFeeGroupNotFoundException(id));
        group.editSnapshot(command.yearlyFee(), command.rules());
        groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    @Override
    public int relevantFeeYear(LocalDate today) {
        return publicationRepository.findActive(today)
                .map(FeeSelectionCampaign::getYear)
                .orElse(today.getYear());
    }
}
