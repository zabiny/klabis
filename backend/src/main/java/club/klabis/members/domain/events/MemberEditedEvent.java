package club.klabis.members.domain.events;

import club.klabis.domain.AggregateDomainEvent;
import club.klabis.members.domain.Member;

@org.jmolecules.event.annotation.DomainEvent
public class MemberEditedEvent extends AggregateDomainEvent<Member> {

    public MemberEditedEvent(Member aggregate) {
        super(aggregate);
    }
}
