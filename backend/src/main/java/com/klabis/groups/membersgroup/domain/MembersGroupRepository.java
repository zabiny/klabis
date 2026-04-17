package com.klabis.groups.membersgroup.domain;

import com.klabis.groups.common.domain.MembersGroupFilter;
import com.klabis.groups.membersgroup.MembersGroupId;
import com.klabis.groups.membersgroup.domain.MembersGroup;

import java.util.List;
import java.util.Optional;

public interface MembersGroupRepository {

    MembersGroup save(MembersGroup group);

    Optional<MembersGroup> findById(MembersGroupId id);

    List<MembersGroup> findAll(MembersGroupFilter filter);

    Optional<MembersGroup> findOne(MembersGroupFilter filter);

    boolean exists(MembersGroupFilter filter);

    boolean existsById(MembersGroupId id);

    void delete(MembersGroupId id);
}
