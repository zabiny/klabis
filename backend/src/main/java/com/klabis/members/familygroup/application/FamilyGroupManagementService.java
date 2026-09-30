package com.klabis.members.familygroup.application;

import com.klabis.common.groups.domain.GroupNotFoundException;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.FamilyGroupId;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupFilter;
import com.klabis.members.familygroup.domain.FamilyGroupRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class FamilyGroupManagementService implements FamilyGroupManagementPort {

    private final FamilyGroupRepository familyGroupRepository;

    FamilyGroupManagementService(FamilyGroupRepository familyGroupRepository) {
        this.familyGroupRepository = familyGroupRepository;
    }

    @Transactional
    @Override
    public FamilyGroup createFamilyGroup(FamilyGroup.CreateFamilyGroup command) {
        validateNotInOtherFamilyGroup(command.parent(), null);
        FamilyGroup group = FamilyGroup.create(command);
        return familyGroupRepository.save(group);
    }

    @Transactional(readOnly = true)
    @Override
    public List<FamilyGroup> listFamilyGroups() {
        return familyGroupRepository.findAll(FamilyGroupFilter.all());
    }

    @Transactional(readOnly = true)
    @Override
    public FamilyGroup getFamilyGroup(FamilyGroupId id) {
        return loadGroup(id);
    }

    @Transactional
    @Override
    public void deleteFamilyGroup(FamilyGroupId id) {
        loadGroup(id);
        familyGroupRepository.delete(id);
    }

    @Transactional
    @Override
    public void addParent(FamilyGroupId id, UserId parent) {
        FamilyGroup group = loadGroup(id);
        validateNotInOtherFamilyGroup(parent, id);
        group.addParent(parent);
        familyGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void removeParent(FamilyGroupId id, UserId parent) {
        FamilyGroup group = loadGroup(id);
        group.removeParent(parent);
        familyGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void addChild(FamilyGroupId id, MemberId child) {
        FamilyGroup group = loadGroup(id);
        validateNotInOtherFamilyGroup(child.toUserId(), id);
        group.addChild(child);
        familyGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void removeChild(FamilyGroupId id, MemberId child) {
        FamilyGroup group = loadGroup(id);
        group.removeChild(child);
        familyGroupRepository.save(group);
    }

    private FamilyGroup loadGroup(FamilyGroupId id) {
        return familyGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException("Family", id));
    }

    // Membership within the target group itself is left to the aggregate, which promotes a child to parent in place.
    private void validateNotInOtherFamilyGroup(UserId userId, FamilyGroupId targetGroup) {
        familyGroupRepository.findOne(FamilyGroupFilter.all().withMemberOrParentIs(userId))
                .filter(existing -> !existing.getId().equals(targetGroup))
                .ifPresent(existing -> {
                    throw new MemberAlreadyInFamilyGroupException(userId);
                });
    }
}
