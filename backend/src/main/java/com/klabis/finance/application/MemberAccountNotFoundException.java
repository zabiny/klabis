package com.klabis.finance.application;

import com.klabis.members.MemberId;

public class MemberAccountNotFoundException extends RuntimeException {

    public MemberAccountNotFoundException(MemberId memberId) {
        super("Member account not found for member: " + memberId.uuid());
    }
}
