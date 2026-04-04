package com.klabis.members.familygroup.application;

import com.klabis.members.MemberId;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupId;
import com.klabis.members.familygroup.domain.FamilyGroupRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
// Temporary qualifier to avoid bean name conflict with usergroups.GroupManagementService — remove in Phase 5 when old module is deleted
@Component("membersFamilyGroupManagementService")
class FamilyGroupManagementService implements FamilyGroupManagementPort {

    private final FamilyGroupRepository familyGroupRepository;

    FamilyGroupManagementService(FamilyGroupRepository familyGroupRepository) {
        this.familyGroupRepository = familyGroupRepository;
    }

    @Transactional
    @Override
    public FamilyGroup createFamilyGroup(FamilyGroup.CreateFamilyGroup command) {
        command.parents().forEach(this::validateNoExistingFamilyGroup);
        command.initialMembers().forEach(this::validateNoExistingFamilyGroup);
        FamilyGroup group = FamilyGroup.create(command);
        return familyGroupRepository.save(group);
    }

    @Transactional(readOnly = true)
    @Override
    public List<FamilyGroup> listFamilyGroups() {
        return familyGroupRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Override
    public FamilyGroup getFamilyGroup(FamilyGroupId id) {
        return familyGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
    }

    @Transactional
    @Override
    public void deleteFamilyGroup(FamilyGroupId id) {
        familyGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
        familyGroupRepository.delete(id);
    }

    @Transactional
    @Override
    public void addParent(FamilyGroupId id, MemberId parent) {
        FamilyGroup group = familyGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
        group.addParent(parent);
        familyGroupRepository.save(group);
    }

    @Transactional
    @Override
    public void removeParent(FamilyGroupId id, MemberId parent) {
        FamilyGroup group = familyGroupRepository.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
        group.removeParent(parent);
        familyGroupRepository.save(group);
    }

    private void validateNoExistingFamilyGroup(MemberId memberId) {
        familyGroupRepository.findByMemberOrParent(memberId).ifPresent(existing -> {
            throw new MemberAlreadyInFamilyGroupException(memberId);
        });
    }
}
