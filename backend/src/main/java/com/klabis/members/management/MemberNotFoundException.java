package com.klabis.members.management;

import com.klabis.common.exceptions.ResourceNotFoundException;

import java.util.UUID;

/**
 * Exception thrown when a member cannot be found by their ID.
 */
class MemberNotFoundException extends ResourceNotFoundException {

    private final UUID memberId;

    public MemberNotFoundException(UUID memberId) {
        super("Member not found with ID: " + memberId);
        this.memberId = memberId;
    }

    public UUID getMemberId() {
        return memberId;
    }
}
