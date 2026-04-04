package com.klabis.members.familygroup.domain;

import com.klabis.members.MemberId;

import java.util.List;
import java.util.Optional;

public interface FamilyGroupRepository {

    FamilyGroup save(FamilyGroup group);

    Optional<FamilyGroup> findById(FamilyGroupId id);

    List<FamilyGroup> findAll();

    Optional<FamilyGroup> findByMemberOrParent(MemberId memberId);

    void delete(FamilyGroupId id);
}
