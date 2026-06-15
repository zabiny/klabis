package com.klabis.membershipfees;

import com.klabis.members.MemberId;
import org.jmolecules.event.annotation.DomainEvent;

/**
 * Published when a member has not chosen a fee level before the voting deadline.
 * <p>
 * The {@code events} module reacts to this event by blocking the member's new registrations
 * and auto-unregistering them from events with open registrations.
 */
@DomainEvent
public record MemberMissedFeeSelectionEvent(MemberId memberId, int year) {
}
