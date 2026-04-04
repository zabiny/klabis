package com.klabis.members.familygroup.application;

import com.klabis.members.MemberId;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface FamilyGroupManagementPort {

    FamilyGroup createFamilyGroup(FamilyGroup.CreateFamilyGroup command);

    List<FamilyGroup> listFamilyGroups();

    FamilyGroup getFamilyGroup(FamilyGroupId id);

    void deleteFamilyGroup(FamilyGroupId id);

    void addParent(FamilyGroupId id, MemberId parent);

    void removeParent(FamilyGroupId id, MemberId parent);
}
