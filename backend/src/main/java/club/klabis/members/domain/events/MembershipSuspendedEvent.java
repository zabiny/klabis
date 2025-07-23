package club.klabis.members.domain.events;

import club.klabis.domain.AggregateDomainEvent;
import club.klabis.members.domain.Member;

@org.jmolecules.event.annotation.DomainEvent
public class MembershipSuspendedEvent extends AggregateDomainEvent<Member> {
    public MembershipSuspendedEvent(Member suspendedMember) {
        super(suspendedMember);
    }
}
