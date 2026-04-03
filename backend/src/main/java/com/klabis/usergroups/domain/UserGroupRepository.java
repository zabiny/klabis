package com.klabis.usergroups.domain;

import com.klabis.usergroups.UserGroupId;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGroupRepository {

    UserGroup save(UserGroup userGroup);

    Optional<UserGroup> findById(UserGroupId id);

    List<UserGroup> findAll(GroupFilter filter);

    Optional<UserGroup> findOne(GroupFilter filter);

    void delete(UserGroupId id);
}
