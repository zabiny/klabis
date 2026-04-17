package com.klabis.groups.freegroup.domain;

import com.klabis.groups.common.domain.FreeGroupFilter;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.domain.FreeGroup;

import java.util.List;
import java.util.Optional;

public interface FreeGroupRepository {

    FreeGroup save(FreeGroup group);

    Optional<FreeGroup> findById(FreeGroupId id);

    List<FreeGroup> findAll(FreeGroupFilter filter);

    Optional<FreeGroup> findOne(FreeGroupFilter filter);

    boolean exists(FreeGroupFilter filter);

    boolean existsById(FreeGroupId id);

    void delete(FreeGroupId id);
}
