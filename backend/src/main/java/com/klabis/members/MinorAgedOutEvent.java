package com.klabis.members;

import org.jmolecules.event.annotation.DomainEvent;

import java.util.Objects;

/**
 * Published when a member reaches the age of 18, so that parts of the system tracking minors can let go of them.
 */
@DomainEvent
public record MinorAgedOutEvent(MemberId memberId) {

    public MinorAgedOutEvent {
        Objects.requireNonNull(memberId, "Member ID is required");
    }
}
