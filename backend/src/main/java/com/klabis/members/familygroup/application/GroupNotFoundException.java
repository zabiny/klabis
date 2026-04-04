package com.klabis.members.familygroup.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.members.familygroup.domain.FamilyGroupId;

public class GroupNotFoundException extends ResourceNotFoundException {

    public GroupNotFoundException(FamilyGroupId id) {
        super("Family group not found with ID: " + id);
    }
}
