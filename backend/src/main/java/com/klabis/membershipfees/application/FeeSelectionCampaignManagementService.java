package com.klabis.membershipfees.application;

import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
class FeeSelectionCampaignManagementService implements FeeSelectionCampaignManagementPort {

    private final FeeSelectionCampaignRepository publicationRepository;
    private final MembershipFeeGroupRepository groupRepository;
    private final MembershipFeeTierRepository tierRepository;

    FeeSelectionCampaignManagementService(FeeSelectionCampaignRepository publicationRepository,
                                          MembershipFeeGroupRepository groupRepository,
                                          MembershipFeeTierRepository tierRepository) {
        this.publicationRepository = publicationRepository;
        this.groupRepository = groupRepository;
        this.tierRepository = tierRepository;
    }

    @Transactional
    @Override
    public FeeSelectionCampaignId publishYear(PublishYearCommand command) {
        publicationRepository.findByYear(command.year()).ifPresent(existing -> {
            throw new ActiveCampaignExistsException(command.year());
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
    public void editGroupSnapshot(MembershipFeeGroupId id, EditGroupSnapshotCommand command) {
        MembershipFeeGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new MembershipFeeGroupNotFoundException(id));
        group.editSnapshot(command.yearlyFee(), command.rules());
        groupRepository.save(group);
    }
}
