package com.klabis.members.legalguardian.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.common.users.UserId;

public class LegalGuardianNotFoundException extends ResourceNotFoundException implements UserFacingError {

    public LegalGuardianNotFoundException(UserId id) {
        super("Legal guardian not found with ID: " + id.uuid());
    }
}
