package com.klabis.usergroups.application;

import com.klabis.members.ActiveMembersByAgeProvider;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.AgeRange;
import com.klabis.usergroups.domain.GroupFilter;
import com.klabis.usergroups.domain.GroupType;
import com.klabis.usergroups.domain.TrainingGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TrainingGroupManagementService implements TrainingGroupManagementPort {

    private final UserGroupRepository userGroupRepository;
    private final ActiveMembersByAgeProvider activeMembersByAgeProvider;

    TrainingGroupManagementService(UserGroupRepository userGroupRepository,
                                    ActiveMembersByAgeProvider activeMembersByAgeProvider) {
        this.userGroupRepository = userGroupRepository;
        this.activeMembersByAgeProvider = activeMembersByAgeProvider;
    }

    @Transactional
    @Override
    public TrainingGroup createTrainingGroup(TrainingGroup.CreateTrainingGroup command) {
        validateNoOverlappingAgeRange(command.ageRange(), null);
        TrainingGroup group = TrainingGroup.create(command);
        activeMembersByAgeProvider
                .findActiveMemberIdsByAgeRange(command.ageRange().minAge(), command.ageRange().maxAge())
                .forEach(group::assignEligibleMember);
        return (TrainingGroup) userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public TrainingGroup updateTrainingGroup(UserGroupId id, UpdateTrainingGroupCommand command) {
        TrainingGroup group = loadTrainingGroup(id);
        command.name().ifProvided(group::rename);
        if (command.minAge().isProvided()) {
            AgeRange newAgeRange = new AgeRange(command.minAge().throwIfNotProvided(), command.maxAge().throwIfNotProvided());
            validateNoOverlappingAgeRange(newAgeRange, id);
            group.updateAgeRange(newAgeRange);
        }
        command.trainers().ifProvided(group::replaceTrainers);
        return (TrainingGroup) userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void deleteTrainingGroup(UserGroupId id) {
        loadTrainingGroup(id);
        userGroupRepository.delete(id);
    }

    @Transactional
    @Override
    public void addTrainer(UserGroupId id, MemberId trainerId) {
        TrainingGroup group = loadTrainingGroup(id);
        group.addTrainer(trainerId);
        userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void removeTrainer(UserGroupId id, MemberId trainerId) {
        TrainingGroup group = loadTrainingGroup(id);
        group.removeTrainer(trainerId);
        userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void addMemberToTrainingGroup(UserGroupId id, MemberId memberId) {
        TrainingGroup group = loadTrainingGroup(id);
        group.assignEligibleMember(memberId);
        userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void removeMemberFromTrainingGroup(UserGroupId id, MemberId memberId) {
        TrainingGroup group = loadTrainingGroup(id);
        group.removeMember(memberId);
        userGroupRepository.save(group);
    }

    private TrainingGroup loadTrainingGroup(UserGroupId id) {
        return userGroupRepository.findById(id)
                .filter(g -> g instanceof TrainingGroup)
                .map(g -> (TrainingGroup) g)
                .orElseThrow(() -> new GroupNotFoundException(id));
    }

    private void validateNoOverlappingAgeRange(AgeRange ageRange, UserGroupId excludeId) {
        userGroupRepository.findAll(GroupFilter.byType(GroupType.TRAINING)).stream()
                .map(g -> (TrainingGroup) g)
                .filter(g -> excludeId == null || !g.getId().equals(excludeId))
                .filter(g -> g.getAgeRange().overlaps(ageRange))
                .findFirst()
                .ifPresent(g -> {
                    throw new AgeRange.OverlappingAgeRangeException(g.getAgeRange());
                });
    }
}
