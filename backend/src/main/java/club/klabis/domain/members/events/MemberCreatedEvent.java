package club.klabis.domain.members.events;

import club.klabis.domain.AggregateDomainEvent;
import club.klabis.domain.members.Member;

@org.jmolecules.event.annotation.DomainEvent
public class MemberCreatedEvent extends AggregateDomainEvent<Member> {

    public MemberCreatedEvent(Member aggregate) {
        super(aggregate);
    }
}
