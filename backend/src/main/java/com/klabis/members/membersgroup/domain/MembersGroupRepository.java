package com.klabis.members.membersgroup.domain;

import com.klabis.groups.common.domain.MembersGroupFilter;

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
