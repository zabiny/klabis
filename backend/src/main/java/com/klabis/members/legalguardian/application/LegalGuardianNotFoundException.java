package com.klabis.members.legalguardian.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.users.UserId;

public class LegalGuardianNotFoundException extends ResourceNotFoundException {

    public LegalGuardianNotFoundException(UserId id) {
        super("Legal guardian not found with ID: " + id.uuid());
    }
}
