package com.klabis.members.membersgroup.domain;

import com.klabis.members.MemberId;

import java.util.List;
import java.util.Optional;

public interface MembersGroupRepository {

    MembersGroup save(MembersGroup group);

    Optional<MembersGroup> findById(MembersGroupId id);

    List<MembersGroup> findGroupsForMember(MemberId memberId);

    List<MembersGroup> findGroupsWithPendingInvitationsForMember(MemberId memberId);

    void delete(MembersGroupId id);
}
