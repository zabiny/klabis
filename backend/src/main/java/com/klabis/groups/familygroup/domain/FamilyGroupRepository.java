package com.klabis.groups.familygroup.domain;

import com.klabis.groups.common.domain.FamilyGroupFilter;
import com.klabis.groups.familygroup.FamilyGroupId;

import java.util.List;
import java.util.Optional;

public interface FamilyGroupRepository {

    FamilyGroup save(FamilyGroup group);

    Optional<FamilyGroup> findById(FamilyGroupId id);

    List<FamilyGroup> findAll(FamilyGroupFilter filter);

    Optional<FamilyGroup> findOne(FamilyGroupFilter filter);

    boolean exists(FamilyGroupFilter filter);

    void delete(FamilyGroupId id);
}
