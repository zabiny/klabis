package club.klabis.domain.members.events;

import club.klabis.domain.DomainEvent;
import club.klabis.domain.members.Member;

@org.jmolecules.event.annotation.DomainEvent
public class MemberCreatedEvent extends DomainEvent<Member> {

    public MemberCreatedEvent(Member aggregate) {
        super(aggregate);
    }
}
