package com.klabis.membershipfees;

import com.klabis.members.MemberId;
import org.jmolecules.event.annotation.DomainEvent;

/**
 * Published when an administrator performs an emergency fee assignment for a member who was
 * previously sanctioned (blocked) for missing the fee selection deadline.
 * <p>
 * The {@code events} module reacts to this event by unblocking the member's future event
 * registrations. Previously auto-cancelled registrations are NOT restored automatically —
 * manual re-registration is required per design decision D9.
 */
@DomainEvent
public record MemberFeeSelectionResolvedEvent(MemberId memberId, int year) {
}
