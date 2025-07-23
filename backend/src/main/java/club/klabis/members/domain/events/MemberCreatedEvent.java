package club.klabis.members.domain.events;

import club.klabis.domain.AggregateDomainEvent;
import club.klabis.members.domain.Member;

@org.jmolecules.event.annotation.DomainEvent
public class MemberCreatedEvent extends AggregateDomainEvent<Member> {

    public MemberCreatedEvent(Member aggregate) {
        super(aggregate);
    }
}
