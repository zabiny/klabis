package com.klabis.members.traininggroup.application;

import com.klabis.members.ActiveMembersByAgeProvider;
import com.klabis.members.MemberId;
import com.klabis.common.usergroup.GroupNotFoundException;
import com.klabis.members.traininggroup.domain.AgeRange;
import com.klabis.members.traininggroup.domain.TrainingGroup;
import com.klabis.members.traininggroup.domain.TrainingGroupId;
import com.klabis.members.traininggroup.domain.TrainingGroupRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        return trainingGroupRepository.findAll();
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

    @Transactional
    @Override
    public TrainingGroup updateTrainingGroup(TrainingGroupId id, UpdateTrainingGroupCommand command) {
        TrainingGroup group = loadTrainingGroup(id);
        command.name().ifProvided(group::rename);
        if (command.minAge().isProvided()) {
            AgeRange newAgeRange = new AgeRange(command.minAge().throwIfNotProvided(), command.maxAge().throwIfNotProvided());
            validateNoOverlappingAgeRange(newAgeRange, id);
            group.updateAgeRange(newAgeRange);
        }
        command.trainers().ifProvided(group::replaceTrainers);
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
        trainingGroupRepository.findAll().stream()
                .filter(g -> excludeId == null || !g.getId().equals(excludeId))
                .filter(g -> g.getAgeRange().overlaps(ageRange))
                .findFirst()
                .ifPresent(g -> {
                    throw new AgeRange.OverlappingAgeRangeException(g.getAgeRange());
                });
    }
}
