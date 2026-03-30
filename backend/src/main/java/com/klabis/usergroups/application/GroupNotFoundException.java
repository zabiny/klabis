package com.klabis.usergroups.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.usergroups.UserGroupId;

public class GroupNotFoundException extends ResourceNotFoundException {

    public GroupNotFoundException(UserGroupId id) {
        super("User group not found with ID: " + id);
    }
}
