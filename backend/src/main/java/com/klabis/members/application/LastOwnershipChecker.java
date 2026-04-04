package com.klabis.members.application;

import com.klabis.members.MemberId;

import java.util.List;

public interface LastOwnershipChecker {

    List<OwnedGroupInfo> findGroupsOwnedSolely(MemberId memberId);

    record OwnedGroupInfo(String groupId, String groupName, String groupType) {
    }
}
