package com.klabis.members.membersgroup.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.members.membersgroup.domain.MembersGroupId;

public class GroupNotFoundException extends ResourceNotFoundException {

    public GroupNotFoundException(MembersGroupId id) {
        super("Members group not found with ID: " + id);
    }
}
