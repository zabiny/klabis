package com.klabis.membershipfees.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.membershipfees.MembershipFeeGroupId;

public class MembershipFeeGroupNotFoundException extends ResourceNotFoundException implements UserFacingError {

    public MembershipFeeGroupNotFoundException(MembershipFeeGroupId id) {
        super("MembershipFeeGroup not found: " + id.value());
    }
}
