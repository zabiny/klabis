package com.klabis.groups.familygroup.application;

import com.klabis.common.users.UserId;
import com.klabis.groups.common.domain.GroupNotFoundException;
import com.klabis.groups.familygroup.FamilyGroupId;
import com.klabis.groups.familygroup.domain.FamilyGroup;
import com.klabis.groups.familygroup.domain.FamilyGroupFilter;
import com.klabis.groups.familygroup.domain.FamilyGroupRepository;
import com.klabis.members.MemberId;
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
        validateNoExistingFamilyGroup(command.parent());
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
        validateNoExistingFamilyGroup(parent);
        FamilyGroup group = loadGroup(id);
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
        validateNoExistingFamilyGroup(child.toUserId());
        FamilyGroup group = loadGroup(id);
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

    private void validateNoExistingFamilyGroup(UserId userId) {
        familyGroupRepository.findOne(FamilyGroupFilter.all().withMemberOrParentIs(userId)).ifPresent(existing -> {
            throw new MemberAlreadyInFamilyGroupException(userId);
        });
    }
}
