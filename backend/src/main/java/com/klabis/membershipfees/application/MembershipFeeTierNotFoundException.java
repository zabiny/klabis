package com.klabis.membershipfees.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.membershipfees.MembershipFeeTierId;

public class MembershipFeeTierNotFoundException extends ResourceNotFoundException implements UserFacingError {

    public MembershipFeeTierNotFoundException(MembershipFeeTierId id) {
        super("MembershipFeeTier not found: " + id.value());
    }
}
