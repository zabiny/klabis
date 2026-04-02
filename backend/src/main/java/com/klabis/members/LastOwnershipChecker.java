package com.klabis.members;

import java.util.List;

/**
 * Port for checking group ownership before suspending a member.
 * Implemented by the usergroups module to avoid a circular module dependency.
 */
public interface LastOwnershipChecker {

    List<OwnedGroupInfo> findGroupsOwnedSolely(MemberId memberId);

    record OwnedGroupInfo(String groupId, String groupName, String groupType) {
    }
}
