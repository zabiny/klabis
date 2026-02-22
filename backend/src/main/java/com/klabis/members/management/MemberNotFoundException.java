package com.klabis.members.management;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.members.domain.MemberId;

/**
 * Exception thrown when a member cannot be found by their ID.
 */
public class MemberNotFoundException extends ResourceNotFoundException {

    private final MemberId memberId;

    public MemberNotFoundException(MemberId memberId) {
        super("Member not found with ID: " + memberId);
        this.memberId = memberId;
    }

    public MemberId getMemberId() {
        return memberId;
    }
}
