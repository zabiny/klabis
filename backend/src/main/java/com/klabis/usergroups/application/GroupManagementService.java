package com.klabis.usergroups.application;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.AgeRange;
import com.klabis.usergroups.domain.FamilyGroup;
import com.klabis.usergroups.domain.FreeGroup;
import com.klabis.usergroups.domain.TrainingGroup;
import com.klabis.usergroups.domain.UserGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class GroupManagementService implements GroupManagementPort {

    private final UserGroupRepository userGroupRepository;

    GroupManagementService(UserGroupRepository userGroupRepository) {
        this.userGroupRepository = userGroupRepository;
    }

    @Transactional
    @Override
    public UserGroup createFreeGroup(FreeGroup.CreateFreeGroup command) {
        FreeGroup group = FreeGroup.create(command);
        return userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public FamilyGroup createFamilyGroup(FamilyGroup.CreateFamilyGroup command) {
        validateNoExistingFamilyGroup(command.owner());
        command.initialMembers().forEach(this::validateNoExistingFamilyGroup);
        FamilyGroup group = FamilyGroup.create(command);
        return (FamilyGroup) userGroupRepository.save(group);
    }

    @Transactional(readOnly = true)
    @Override
    public List<FamilyGroup> listFamilyGroups() {
        return userGroupRepository.findAllFamilyGroups();
    }

    @Transactional(readOnly = true)
    @Override
    public FamilyGroup getFamilyGroup(UserGroupId id) {
        UserGroup group = loadGroup(id);
        if (!(group instanceof FamilyGroup familyGroup)) {
            throw new GroupNotFoundException(id);
        }
        return familyGroup;
    }

    @Transactional
    @Override
    public void deleteFamilyGroup(UserGroupId id) {
        UserGroup group = loadGroup(id);
        if (!(group instanceof FamilyGroup)) {
            throw new GroupNotFoundException(id);
        }
        userGroupRepository.delete(id);
    }

    private void validateNoExistingFamilyGroup(MemberId memberId) {
        userGroupRepository.findFamilyGroupByMember(memberId).ifPresent(existing -> {
            throw new MemberAlreadyInFamilyGroupException(memberId);
        });
    }

    @Transactional
    @Override
    public TrainingGroup createTrainingGroup(TrainingGroup.CreateTrainingGroup command) {
        validateNoOverlappingAgeRange(command.ageRange(), null);
        TrainingGroup group = TrainingGroup.create(command);
        return (TrainingGroup) userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public TrainingGroup updateTrainingGroupAgeRange(UserGroupId id, AgeRange newAgeRange, MemberId requestingMember) {
        UserGroup group = loadGroup(id);
        if (!(group instanceof TrainingGroup trainingGroup)) {
            throw new GroupNotFoundException(id);
        }
        requireOwner(trainingGroup, requestingMember);
        validateNoOverlappingAgeRange(newAgeRange, id);
        trainingGroup.updateAgeRange(newAgeRange);
        return (TrainingGroup) userGroupRepository.save(trainingGroup);
    }

    @Transactional(readOnly = true)
    @Override
    public List<TrainingGroup> listTrainingGroups() {
        return userGroupRepository.findAllTrainingGroups();
    }

    private void validateNoOverlappingAgeRange(AgeRange ageRange, UserGroupId excludeId) {
        userGroupRepository.findAllTrainingGroups().stream()
                .filter(g -> excludeId == null || !g.getId().equals(excludeId))
                .filter(g -> g.getAgeRange().overlaps(ageRange))
                .findFirst()
                .ifPresent(g -> {
                    throw new AgeRange.OverlappingAgeRangeException(g.getAgeRange());
                });
    }

    @Transactional(readOnly = true)
    @Override
    public TrainingGroup getTrainingGroup(UserGroupId id) {
        UserGroup group = loadGroup(id);
        if (!(group instanceof TrainingGroup trainingGroup)) {
            throw new GroupNotFoundException(id);
        }
        return trainingGroup;
    }

    @Transactional(readOnly = true)
    @Override
    public UserGroup getGroup(UserGroupId id) {
        return userGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserGroup> listGroupsForMember(MemberId memberId) {
        return userGroupRepository.findAllByMember(memberId);
    }

    @Transactional
    @Override
    public UserGroup renameGroup(UserGroupId id, String newName, MemberId requestingMember) {
        UserGroup group = loadGroup(id);
        requireOwner(group, requestingMember);
        group.rename(newName);
        return userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void deleteGroup(UserGroupId id, MemberId requestingMember) {
        UserGroup group = loadGroup(id);
        requireOwner(group, requestingMember);
        userGroupRepository.delete(id);
    }

    @Transactional
    @Override
    public UserGroup addMemberToGroup(UserGroupId id, MemberId memberToAdd, MemberId requestingMember) {
        UserGroup group = loadGroup(id);
        requireOwner(group, requestingMember);
        group.addMember(memberToAdd);
        return userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public UserGroup removeMemberFromGroup(UserGroupId id, MemberId memberToRemove, MemberId requestingMember) {
        UserGroup group = loadGroup(id);
        requireOwner(group, requestingMember);
        group.removeMember(memberToRemove);
        return userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public UserGroup addOwnerToGroup(UserGroupId id, MemberId newOwner, MemberId requestingMember) {
        UserGroup group = loadGroup(id);
        requireOwner(group, requestingMember);
        group.addOwner(newOwner);
        return userGroupRepository.save(group);
    }

    @Transactional
    @Override
    public UserGroup removeOwnerFromGroup(UserGroupId id, MemberId ownerToRemove, MemberId requestingMember) {
        UserGroup group = loadGroup(id);
        requireOwner(group, requestingMember);
        group.removeOwner(ownerToRemove);
        return userGroupRepository.save(group);
    }

    private UserGroup loadGroup(UserGroupId id) {
        return userGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
    }

    private void requireOwner(UserGroup group, MemberId requestingMember) {
        if (!group.isOwner(requestingMember)) {
            throw new NotGroupOwnerException(requestingMember, group.getId());
        }
    }
}
