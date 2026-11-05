package com.klabis.common.groups.domain;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;

public class GroupNotFoundException extends ResourceNotFoundException implements UserFacingError {

    public GroupNotFoundException(String groupType, Object id) {
        super(groupType + " group not found with ID: " + id);
    }
}
