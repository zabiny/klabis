package com.klabis.finance.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.members.MemberId;

public class MemberAccountNotFoundException extends ResourceNotFoundException implements UserFacingError {

    private final MemberId memberId;

    public MemberAccountNotFoundException(MemberId memberId) {
        super("Member account not found for member: " + memberId.uuid());
        this.memberId = memberId;
    }

    public MemberId memberId() {
        return memberId;
    }
}
