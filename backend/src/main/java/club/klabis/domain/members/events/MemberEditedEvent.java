package club.klabis.domain.members.events;

import club.klabis.domain.AggregateDomainEvent;
import club.klabis.domain.members.Member;

@org.jmolecules.event.annotation.DomainEvent
public class MemberEditedEvent extends AggregateDomainEvent<Member> {

    public MemberEditedEvent(Member aggregate) {
        super(aggregate);
    }
}
