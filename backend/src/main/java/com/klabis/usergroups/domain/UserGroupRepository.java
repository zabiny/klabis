package com.klabis.usergroups.domain;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGroupRepository {

    UserGroup save(UserGroup userGroup);

    Optional<UserGroup> findById(UserGroupId id);

    List<UserGroup> findAllByMember(MemberId memberId);

    List<UserGroup> findAllWithPendingInvitationForMember(MemberId memberId);

    void delete(UserGroupId id);
}
