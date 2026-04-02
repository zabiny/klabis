package com.klabis.usergroups;

import com.klabis.members.MemberId;

import java.util.List;

public interface UserGroups {

    List<UserGroupOwnershipInfo> findGroupsWhereLastOwner(MemberId memberId);
}
