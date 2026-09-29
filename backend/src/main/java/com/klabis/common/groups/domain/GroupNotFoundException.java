package com.klabis.common.groups.domain;

import com.klabis.common.exceptions.ResourceNotFoundException;

public class GroupNotFoundException extends ResourceNotFoundException {

    public GroupNotFoundException(String groupType, Object id) {
        super(groupType + " group not found with ID: " + id);
    }
}
