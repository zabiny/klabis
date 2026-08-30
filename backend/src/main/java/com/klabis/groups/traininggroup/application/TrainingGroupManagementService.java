package com.klabis.groups.traininggroup.application;

import com.klabis.groups.common.domain.GroupNotFoundException;
import com.klabis.groups.common.domain.AgeRangeOverlap;
import com.klabis.groups.common.domain.TrainingGroupFilter;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import com.klabis.members.ActiveMembersByAgeProvider;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
class TrainingGroupManagementService implements TrainingGroupManagementPort {

    private final TrainingGroupRepository trainingGroupRepository;
    private final ActiveMembersByAgeProvider activeMembersByAgeProvider;

    TrainingGroupManagementService(TrainingGroupRepository trainingGroupRepository,
                                   ActiveMembersByAgeProvider activeMembersByAgeProvider) {
        this.trainingGroupRepository = trainingGroupRepository;
        this.activeMembersByAgeProvider = activeMembersByAgeProvider;
    }

    @Transactional(readOnly = true)
    @Override
    public List<TrainingGroup> listTrainingGroups() {
        return trainingGroupRepository.findAll(TrainingGroupFilter.all());
    }

    @Transactional(readOnly = true)
    @Override
    public TrainingGroup getTrainingGroup(TrainingGroupId id) {
        return loadTrainingGroup(id);
    }

    @Transactional
    @Override
    public TrainingGroup createTrainingGroup(TrainingGroup.CreateTrainingGroup command) {
        validateNoOverlappingAgeRange(command.ageRange(), null);
        TrainingGroup group = TrainingGroup.create(command);
        activeMembersByAgeProvider
                .findActiveMemberIdsByAgeRange(command.ageRange().minAge(), command.ageRange().maxAge())
                .forEach(group::assignEligibleMember);
        return trainingGroupRepository.save(group);
    }

    @Transactional(readOnly = true)
    @Override
    public UpdateTrainingGroupCommand prefilledUpdateCommand(TrainingGroupId id) {
        return UpdateTrainingGroupCommand.from(loadTrainingGroup(id));
    }

    @Transactional
    @Override
    public TrainingGroup updateTrainingGroup(TrainingGroupId id, UpdateTrainingGroupCommand command) {
        TrainingGroup group = loadTrainingGroup(id);

        group.rename(command.name());

        if (!Objects.equals(group.getAgeRange(), command.ageRange())) {
            validateNoOverlappingAgeRange(command.ageRange(), id);
            group.updateAgeRange(command.ageRange());
        }

        group.replaceTrainers(command.trainers());

        return trainingGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void deleteTrainingGroup(TrainingGroupId id) {
        loadTrainingGroup(id);
        trainingGroupRepository.delete(id);
    }

    @Transactional
    @Override
    public void addTrainer(TrainingGroupId id, MemberId trainerId) {
        TrainingGroup group = loadTrainingGroup(id);
        group.addTrainer(trainerId);
        trainingGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void removeTrainer(TrainingGroupId id, MemberId trainerId) {
        TrainingGroup group = loadTrainingGroup(id);
        group.removeTrainer(trainerId);
        trainingGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void addMemberToTrainingGroup(TrainingGroupId id, MemberId memberId) {
        trainingGroupRepository.findOne(TrainingGroupFilter.all().withMemberIs(memberId))
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new MemberAlreadyInTrainingGroupException(memberId, existing.getId());
                });
        TrainingGroup group = loadTrainingGroup(id);
        group.assignEligibleMember(memberId);
        trainingGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void removeMemberFromTrainingGroup(TrainingGroupId id, MemberId memberId) {
        TrainingGroup group = loadTrainingGroup(id);
        group.removeMember(memberId);
        trainingGroupRepository.save(group);
    }

    private TrainingGroup loadTrainingGroup(TrainingGroupId id) {
        return trainingGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException("Training", id));
    }

    private void validateNoOverlappingAgeRange(AgeRange ageRange, TrainingGroupId excludeId) {
        if (trainingGroupRepository.exists(TrainingGroupFilter.all().withOverlap(new AgeRangeOverlap(ageRange, excludeId)))) {
            throw new AgeRange.OverlappingAgeRangeException(ageRange);
        }
    }
}
