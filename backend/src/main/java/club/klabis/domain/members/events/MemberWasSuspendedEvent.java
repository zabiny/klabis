package club.klabis.domain.members.events;

import club.klabis.domain.DomainEvent;
import club.klabis.domain.members.Member;

@org.jmolecules.event.annotation.DomainEvent
public class MemberWasSuspendedEvent extends DomainEvent<Member> {
    public MemberWasSuspendedEvent(Member suspendedMember) {
        super(suspendedMember);
    }
}
