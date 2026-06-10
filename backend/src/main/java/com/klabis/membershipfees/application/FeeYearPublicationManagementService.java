package com.klabis.membershipfees.application;

import com.klabis.membershipfees.FeeYearPublicationId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
class FeeYearPublicationManagementService implements FeeYearPublicationManagementPort {

    private final FeeYearPublicationRepository publicationRepository;
    private final MembershipFeeGroupRepository groupRepository;
    private final MembershipFeeTierRepository levelRepository;

    FeeYearPublicationManagementService(FeeYearPublicationRepository publicationRepository,
                                         MembershipFeeGroupRepository groupRepository,
                                         MembershipFeeTierRepository levelRepository) {
        this.publicationRepository = publicationRepository;
        this.groupRepository = groupRepository;
        this.levelRepository = levelRepository;
    }

    @Transactional
    @Override
    public FeeYearPublicationId publishYear(PublishYearCommand command) {
        publicationRepository.findByYear(command.year()).ifPresent(existing -> {
            throw new DuplicateYearPublicationException(command.year());
        });

        List<MembershipFeeTier> levels = command.levelIds().stream()
                .map(id -> levelRepository.findById(id)
                        .orElseThrow(() -> new MembershipFeeTierNotFoundException(id)))
                .toList();

        FeeYearPublication.FeeYearPublicationWithGroups result = FeeYearPublication.publish(
                command.year(), command.votingDeadline(), levels);

        for (MembershipFeeGroup group : result.groups()) {
            groupRepository.save(group);
        }

        return publicationRepository.save(result.publication()).getId();
    }

    @Transactional(readOnly = true)
    @Override
    public FeeYearPublication getPublication(FeeYearPublicationId id) {
        return publicationRepository.findById(id)
                .orElseThrow(() -> new FeeYearPublicationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<FeeYearPublication> getPublicationForYear(int year) {
        return publicationRepository.findByYear(year);
    }

    @Transactional(readOnly = true)
    @Override
    public List<FeeYearPublication> listPublications() {
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
