package com.klabis.members.familygroup.application;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.FamilyGroupId;
import com.klabis.members.familygroup.domain.FamilyGroup;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface FamilyGroupManagementPort {

    FamilyGroup createFamilyGroup(FamilyGroup.CreateFamilyGroup command);

    List<FamilyGroup> listFamilyGroups();

    FamilyGroup getFamilyGroup(FamilyGroupId id);

    void deleteFamilyGroup(FamilyGroupId id);

    void addParent(FamilyGroupId id, UserId parent);

    void removeParent(FamilyGroupId id, UserId parent);

    void addChild(FamilyGroupId id, MemberId child);

    void removeChild(FamilyGroupId id, MemberId child);
}
