package com.klabis.members.familygroup.application;

import com.klabis.common.usergroup.GroupNotFoundException;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupId;
import com.klabis.members.familygroup.domain.FamilyGroupRepository;
import com.klabis.groups.common.domain.FamilyGroupFilter;
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
    public void addParent(FamilyGroupId id, MemberId parent) {
        validateNoExistingFamilyGroup(parent);
        FamilyGroup group = loadGroup(id);
        group.addParent(parent);
        familyGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void removeParent(FamilyGroupId id, MemberId parent) {
        FamilyGroup group = loadGroup(id);
        group.removeParent(parent);
        familyGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void addChild(FamilyGroupId id, MemberId child) {
        validateNoExistingFamilyGroup(child);
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

    private void validateNoExistingFamilyGroup(MemberId memberId) {
        familyGroupRepository.findOne(FamilyGroupFilter.all().withMemberOrParentIs(memberId)).ifPresent(existing -> {
            throw new MemberAlreadyInFamilyGroupException(memberId);
        });
    }
}
