package com.klabis.members.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.members.MemberId;

/**
 * Exception thrown when a member cannot be found by their ID.
 */
public class MemberNotFoundException extends ResourceNotFoundException {

    private final MemberId memberId;

    public MemberNotFoundException(MemberId memberId) {
        super("Member not found with ID: " + memberId);
        this.memberId = memberId;
    }

    /**
     * Used when a member is looked up by an external system's own id (e.g. an ORIS
     * club member id during synchronisation) rather than the Klabis {@link MemberId} —
     * mirrors {@code DisciplineNotFoundException}'s external-id overload.
     */
    public MemberNotFoundException(String externalId) {
        super("ORIS club member not found with ID: " + externalId);
        this.memberId = null;
    }

    public MemberId getMemberId() {
        return memberId;
    }
}
