package club.klabis.domain.members.events;

import club.klabis.domain.AggregateDomainEvent;
import club.klabis.domain.members.Member;

@org.jmolecules.event.annotation.DomainEvent
public class MembershipSuspendedEvent extends AggregateDomainEvent<Member> {
    public MembershipSuspendedEvent(Member suspendedMember) {
        super(suspendedMember);
    }
}
