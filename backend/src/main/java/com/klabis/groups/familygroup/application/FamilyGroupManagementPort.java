package com.klabis.groups.familygroup.application;

import com.klabis.groups.familygroup.FamilyGroupId;
import com.klabis.groups.familygroup.domain.FamilyGroup;
import com.klabis.members.MemberId;
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

    void addChild(FamilyGroupId id, MemberId child);

    void removeChild(FamilyGroupId id, MemberId child);
}
